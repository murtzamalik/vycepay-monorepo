package com.vycepay.common.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that loads ChoiceBankCallbackSignatureVerifier only when
 * verify-signature is true AND private-key is configured.
 */
public class CallbackSignatureVerificationCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String verify = context.getEnvironment().getProperty("vycepay.callback.verify-signature", "false");
        if (!"true".equalsIgnoreCase(verify)) {
            return false;
        }
        String privateKey = context.getEnvironment().getProperty("vycepay.choice-bank.private-key", "");
        return privateKey != null && !privateKey.isBlank();
    }
}
