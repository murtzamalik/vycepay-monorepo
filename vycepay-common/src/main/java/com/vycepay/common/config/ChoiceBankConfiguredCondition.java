package com.vycepay.common.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that loads Choice Bank beans only when sender-id and private-key
 * are non-empty. Prevents loading with empty credentials.
 */
public class ChoiceBankConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String senderId = context.getEnvironment().getProperty("vycepay.choice-bank.sender-id", "");
        String privateKey = context.getEnvironment().getProperty("vycepay.choice-bank.private-key", "");
        return senderId != null && !senderId.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }
}
