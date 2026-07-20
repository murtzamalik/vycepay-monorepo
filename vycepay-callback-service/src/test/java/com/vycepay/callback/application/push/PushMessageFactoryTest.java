package com.vycepay.callback.application.push;

import com.vycepay.callback.domain.model.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Push copy/data mapping from live Choice Bank callback shapes.
 */
class PushMessageFactoryTest {

    private PushMessageFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PushMessageFactory();
    }

    @Test
    void profileCheck_0024_usesResultDescription() {
        Map<String, Object> params = new HashMap<>();
        params.put("onboardingRequestId", "ONBRD02880436c3402000");
        params.put("profileCheckStatus", "validate");
        params.put("resultCode", "PCS0810");
        params.put("resultDescription", "Document Verified");

        PushMessage msg = factory.create("0024", params);

        assertNotNull(msg);
        assertEquals(PushMessageFactory.PUSH_KYC_DOCUMENT_CHECK, msg.getPushType());
        assertEquals("Document verification", msg.getTitle());
        assertEquals("Document Verified", msg.getBody());
        assertEquals("PCS0810", msg.getData().get("resultCode"));
        assertEquals("ONBRD02880436c3402000", msg.getData().get("onboardingRequestId"));
    }

    @Test
    void onboarding_0001_status7_walletReady() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", 7);
        params.put("userId", "6dc3651f-e93b-4efa-9fe0-ce3b36d2922a");
        params.put("accountId", "46012001327510");
        params.put("onboardingRequestId", "ONBRD02880436c3402000");

        PushMessage msg = factory.create("0001", params);

        assertNotNull(msg);
        assertEquals(PushMessageFactory.PUSH_KYC_ONBOARDING_RESULT, msg.getPushType());
        assertEquals("Account ready", msg.getTitle());
        assertTrue(msg.getBody().contains("wallet"));
        assertEquals("7", msg.getData().get("status"));
    }

    @Test
    void transaction_0002_successDeposit() {
        Map<String, Object> params = new HashMap<>();
        params.put("txId", "UTRANS02880586c4b020018212");
        params.put("amount", "50.00");
        params.put("currency", "KES");
        params.put("txStatus", 8);
        params.put("paymentChannel", "PAY_BILL");

        PushMessage msg = factory.create("0002", params);

        assertNotNull(msg);
        assertEquals(PushMessageFactory.PUSH_TRANSACTION_RESULT, msg.getPushType());
        assertEquals("Money received", msg.getTitle());
        assertEquals("Deposit of KES 50.00 completed", msg.getBody());
        assertEquals("UTRANS02880586c4b020018212", msg.getData().get("txId"));
        assertEquals("8", msg.getData().get("txStatus"));
    }

    @Test
    void transaction_0002_failedStk() {
        Map<String, Object> params = new HashMap<>();
        params.put("txId", "UTRANS028a67c3647020008212");
        params.put("amount", "50.00");
        params.put("currency", "KES");
        params.put("txStatus", 4);
        params.put("errorCode", "TXERR0028");
        params.put("errorMsg", "The User cannot be reached by STK Push");
        params.put("paymentChannel", "PAY_BILL");

        PushMessage msg = factory.create("0002", params);

        assertNotNull(msg);
        assertEquals("Transaction failed", msg.getTitle());
        assertEquals("The User cannot be reached by STK Push", msg.getBody());
        assertEquals("4", msg.getData().get("txStatus"));
    }

    @Test
    void transaction_0002_internalTransferOutbound() {
        Map<String, Object> params = new HashMap<>();
        params.put("txId", "UTRANS02880c6f81c020008212");
        params.put("amount", "-30.00");
        params.put("currency", "KES");
        params.put("txStatus", 8);
        params.put("paymentChannel", "INTERNAL_TRANSFER");
        params.put("oppoAccountName", "DERRICK GWEHONA MUDAKI");

        PushMessage msg = factory.create("0002", params);

        assertNotNull(msg);
        assertEquals("Money sent", msg.getTitle());
        assertEquals("You sent KES 30.00 to DERRICK GWEHONA MUDAKI", msg.getBody());
    }

    @Test
    void statement_0015_includesFileUrlInData() {
        Map<String, Object> params = new HashMap<>();
        params.put("jobId", "DSJ1829577311702302728212");
        params.put("fileUrl", "https://choice-baas.s3.af-south-1.amazonaws.com/baas/vyce/statement.PDF");

        PushMessage msg = factory.create("0015", params);

        assertNotNull(msg);
        assertEquals(PushMessageFactory.PUSH_STATEMENT_READY, msg.getPushType());
        assertEquals("Your statement is ready", msg.getTitle());
        assertEquals("DSJ1829577311702302728212", msg.getData().get("jobId"));
        assertTrue(msg.getData().get("fileUrl").contains("statement.PDF"));
        assertTrue(!msg.getBody().contains("https://"));
    }

    @Test
    void statement_withoutFileUrl_returnsNull() {
        Map<String, Object> params = new HashMap<>();
        params.put("jobId", "DSJ123");

        assertNull(factory.create("0015", params));
    }

    @Test
    void balanceChange_0003_skipped() {
        Map<String, Object> params = new HashMap<>();
        params.put("txId", "UTRANS02880586c4b020018212");
        params.put("amount", "50.00");

        assertNull(factory.create("0003", params));
    }

    @Test
    void accountStatus_0021() {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", "46012001327510");
        params.put("accountStatus", 1);

        PushMessage msg = factory.create("0021", params);

        assertNotNull(msg);
        assertEquals(PushMessageFactory.PUSH_ACCOUNT_STATUS, msg.getPushType());
        assertEquals("Account update", msg.getTitle());
        assertTrue(msg.getBody().contains("locked"));
        assertEquals("1", msg.getData().get("accountStatus"));
    }
}
