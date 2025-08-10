package com.xy.core.valid;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MessageTimeValidator implements ConstraintValidator<MessageTimeValid, Long> {

    private static final long MAX_DIFF_MILLIS = 10_000L;

    @Override
    public boolean isValid(Long messageTime, ConstraintValidatorContext context) {
        if (messageTime == null) return true; // @NotNull 由其他注解处理

        long now = System.currentTimeMillis();
        long diff = Math.abs(now - messageTime);
        return diff <= MAX_DIFF_MILLIS;
    }
}
