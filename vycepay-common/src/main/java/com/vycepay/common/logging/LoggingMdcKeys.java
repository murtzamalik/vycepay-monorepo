package com.vycepay.common.logging;

/**
 * Canonical MDC / JSON field names for structured VycePay logs (ECS-aligned where practical).
 * Included as top-level JSON keys via Logback MDC provider.
 */
public final class LoggingMdcKeys {

    public static final String REQUEST_ID = "requestId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String CHOICE_BANK_REQUEST_ID = "choiceBankRequestId";
    public static final String ONBOARDING_REQUEST_ID = "onboardingRequestId";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String CHOICE_TX_ID = "choiceTxId";
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";
    public static final String BIOMETRIC_SESSION_ID = "biometricSessionId";
    public static final String BIOMETRIC_ATTEMPT_ID = "biometricAttemptId";

    public static final String EVENT_DATASET = "event.dataset";
    public static final String EVENT_ACTION = "event.action";
    public static final String EVENT_OUTCOME = "event.outcome";

    public static final String ERROR_CODE = "error.code";
    public static final String ERROR_TYPE = "error.type";
    public static final String ERROR_MESSAGE = "error.message";

    public static final String CHOICE_PATH = "choice.path";
    public static final String CHOICE_CODE = "choice.code";
    public static final String CHOICE_MSG = "choice.msg";

    public static final String LABELS_FLOW = "labels.flow";
    public static final String HTTP_METHOD = "http.method";
    public static final String URL_PATH = "url.path";

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
    public static final String OUTCOME_UNKNOWN = "unknown";

    public static final String DATASET_VYCE_API = "vyce.api";
    public static final String DATASET_CHOICE_BAAS = "choice.baas";
    public static final String DATASET_CHOICE_CALLBACK = "choice.callback";
    public static final String DATASET_VYCE_SMS = "vyce.sms";
    public static final String DATASET_VYCE_PUSH = "vyce.push";
    public static final String DATASET_VYCE_SECURITY = "vyce.security";

    private LoggingMdcKeys() {
    }
}
