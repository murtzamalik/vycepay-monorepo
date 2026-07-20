package com.vycepay.callback.application.push;

import com.vycepay.callback.domain.model.PushMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds user-facing FCM title/body/data from Choice Bank callback params.
 * Types that send push: 0024, 0001, 0002, 0015, 0009, 0021.
 * Type 0003 (balance change) is intentionally skipped — paired with 0002 in live traffic.
 */
@Component
public class PushMessageFactory {

    public static final String PUSH_KYC_DOCUMENT_CHECK = "KYC_DOCUMENT_CHECK";
    public static final String PUSH_KYC_ONBOARDING_RESULT = "KYC_ONBOARDING_RESULT";
    public static final String PUSH_TRANSACTION_RESULT = "TRANSACTION_RESULT";
    public static final String PUSH_STATEMENT_READY = "STATEMENT_READY";
    public static final String PUSH_ACCOUNT_STATUS = "ACCOUNT_STATUS";

    private static final Set<String> SUPPORTED = Set.of("0024", "0001", "0002", "0015", "0009", "0021");
    private static final int TX_STATUS_SUCCESS = 8;
    private static final int TX_STATUS_FAILED = 4;
    private static final int ONBOARDING_ACCOUNT_OPENED = 7;

    /**
     * @return push message, or null when type should not notify (e.g. 0003) or params insufficient
     */
    public PushMessage create(String notificationType, Map<String, Object> params) {
        if (notificationType == null || params == null || !SUPPORTED.contains(notificationType)) {
            return null;
        }
        return switch (notificationType) {
            case "0024" -> forProfileCheck(params);
            case "0001" -> forOnboarding(params);
            case "0002" -> forTransaction(params);
            case "0015", "0009" -> forStatement(notificationType, params);
            case "0021" -> forAccountStatus(params);
            default -> null;
        };
    }

    private PushMessage forProfileCheck(Map<String, Object> params) {
        String description = firstNonBlank(
                getString(params, "resultDescription"),
                "Your documents were reviewed.");
        return PushMessage.builder()
                .notificationType("0024")
                .pushType(PUSH_KYC_DOCUMENT_CHECK)
                .title("Document verification")
                .body(description)
                .putData("onboardingRequestId", getString(params, "onboardingRequestId"))
                .putData("resultCode", getString(params, "resultCode"))
                .putData("profileCheckStatus", getString(params, "profileCheckStatus"))
                .build();
    }

    private PushMessage forOnboarding(Map<String, Object> params) {
        Integer status = getInt(params, "status");
        String title;
        String body;
        if (status != null && status == ONBOARDING_ACCOUNT_OPENED) {
            title = "Account ready";
            body = "Your VycePay wallet is ready.";
        } else {
            title = "KYC update";
            String rejection = formatRejection(params.get("rejectionReasonMsgs"));
            body = rejection != null ? rejection : "Your KYC status was updated. Open the app for details.";
        }
        return PushMessage.builder()
                .notificationType("0001")
                .pushType(PUSH_KYC_ONBOARDING_RESULT)
                .title(title)
                .body(body)
                .putData("onboardingRequestId", getString(params, "onboardingRequestId"))
                .putData("accountId", getString(params, "accountId"))
                .putData("status", status != null ? String.valueOf(status) : null)
                .build();
    }

    private PushMessage forTransaction(Map<String, Object> params) {
        Integer txStatus = getInt(params, "txStatus");
        String amount = getString(params, "amount");
        String currency = firstNonBlank(getString(params, "currency"), "KES");
        String amountLabel = formatAmount(amount, currency);
        String channel = getString(params, "paymentChannel");
        String counterparty = firstNonBlank(
                getString(params, "oppoAccountName"),
                nestedString(params, "extInfo", "counterpartyName"));
        String errorMsg = getString(params, "errorMsg");

        String title;
        String body;
        if (txStatus != null && txStatus == TX_STATUS_SUCCESS) {
            title = isOutbound(amount) ? "Money sent" : "Money received";
            if (isInternalTransfer(channel) && counterparty != null) {
                body = isOutbound(amount)
                        ? "You sent " + amountLabel + " to " + counterparty
                        : "You received " + amountLabel + " from " + counterparty;
            } else if (isPayBill(channel)) {
                body = "Deposit of " + amountLabel + " completed";
            } else {
                body = "Transaction of " + amountLabel + " completed";
            }
        } else if (txStatus != null && txStatus == TX_STATUS_FAILED) {
            title = "Transaction failed";
            body = (errorMsg != null && !errorMsg.isBlank())
                    ? errorMsg
                    : "Your transaction of " + amountLabel + " failed.";
        } else {
            title = "Transaction update";
            body = "Your transaction of " + amountLabel + " was updated.";
        }

        return PushMessage.builder()
                .notificationType("0002")
                .pushType(PUSH_TRANSACTION_RESULT)
                .title(title)
                .body(body)
                .putData("txId", firstNonBlank(
                        getString(params, "txId"),
                        getString(params, "batchId"),
                        getString(params, "utilityTxId")))
                .putData("txStatus", txStatus != null ? String.valueOf(txStatus) : null)
                .putData("amount", amount)
                .putData("currency", currency)
                .putData("paymentChannel", channel)
                .putData("errorCode", getString(params, "errorCode"))
                .build();
    }

    private PushMessage forStatement(String notificationType, Map<String, Object> params) {
        String jobId = firstNonBlank(
                getString(params, "jobId"),
                getString(params, "requestId"),
                getString(params, "statementRequestId"));
        String fileUrl = firstNonBlank(
                getString(params, "fileUrl"),
                getString(params, "statementUrl"),
                getString(params, "downloadUrl"),
                getString(params, "url"));
        if (fileUrl == null || fileUrl.isBlank()) {
            // No download yet — skip noisy intermediate callbacks
            return null;
        }
        return PushMessage.builder()
                .notificationType(notificationType)
                .pushType(PUSH_STATEMENT_READY)
                .title("Your statement is ready")
                .body("Tap to download your account statement.")
                .putData("jobId", jobId)
                .putData("fileUrl", fileUrl)
                .build();
    }

    private PushMessage forAccountStatus(Map<String, Object> params) {
        Integer accountStatus = getInt(params, "accountStatus");
        if (accountStatus == null) {
            accountStatus = getInt(params, "status");
        }
        String statusLabel = mapAccountStatusLabel(accountStatus);
        return PushMessage.builder()
                .notificationType("0021")
                .pushType(PUSH_ACCOUNT_STATUS)
                .title("Account update")
                .body("Your account status is now " + statusLabel + ".")
                .putData("accountId", getString(params, "accountId"))
                .putData("accountStatus", accountStatus != null ? String.valueOf(accountStatus) : null)
                .putData("statusLabel", statusLabel)
                .build();
    }

    private static String mapAccountStatusLabel(Integer choiceStatus) {
        if (choiceStatus == null) {
            return "updated";
        }
        return switch (choiceStatus) {
            case 0 -> "normal";
            case 1 -> "locked";
            case 2 -> "closed";
            default -> "STATUS_" + choiceStatus;
        };
    }

    private static boolean isPayBill(String channel) {
        return channel != null && channel.equalsIgnoreCase("PAY_BILL");
    }

    private static boolean isInternalTransfer(String channel) {
        return channel != null && channel.equalsIgnoreCase("INTERNAL_TRANSFER");
    }

    private static boolean isOutbound(String amount) {
        if (amount == null || amount.isBlank()) {
            return false;
        }
        try {
            return Double.parseDouble(amount.trim()) < 0;
        } catch (NumberFormatException e) {
            return amount.trim().startsWith("-");
        }
    }

    private static String formatAmount(String amount, String currency) {
        if (amount == null || amount.isBlank()) {
            return currency;
        }
        String normalized = amount.startsWith("-") ? amount.substring(1) : amount;
        return currency + " " + normalized;
    }

    @SuppressWarnings("unchecked")
    private static String formatRejection(Object rejectionReasonMsgs) {
        if (rejectionReasonMsgs == null) {
            return null;
        }
        if (rejectionReasonMsgs instanceof List<?> list) {
            if (list.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(o);
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        String s = rejectionReasonMsgs.toString();
        return "null".equalsIgnoreCase(s) || s.isBlank() ? null : s;
    }

    @SuppressWarnings("unchecked")
    private static String nestedString(Map<String, Object> params, String objectKey, String field) {
        Object nested = params.get(objectKey);
        if (!(nested instanceof Map<?, ?> map)) {
            return null;
        }
        Object v = map.get(field);
        return v != null ? v.toString() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String s : values) {
            if (s != null && !s.isBlank() && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return null;
    }

    private static String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString();
        return "null".equalsIgnoreCase(s) ? null : s;
    }

    private static Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
