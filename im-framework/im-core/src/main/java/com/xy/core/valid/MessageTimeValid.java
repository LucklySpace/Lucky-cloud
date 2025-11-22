package com.xy.core.valid;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MessageTimeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageTimeValid {
    String message() default "消息时间戳无效，与当前时间相差不能超过 10 秒";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
