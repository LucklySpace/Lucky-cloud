//package com.xy.server.annotations.lock;
//
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//import java.util.concurrent.TimeUnit;
//import org.springframework.core.annotation.AliasFor;
//import org.springframework.transaction.annotation.Isolation;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Inherited;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//
/// **
// * @author dense
// */
//@Target({ElementType.TYPE, ElementType.METHOD})
//@Retention(RetentionPolicy.RUNTIME)
//@Inherited
//@Transactional
//public @interface LockTransactional {
//    @AliasFor("transactionManager")
//    String value() default "";
//
//    @AliasFor("value")
//    String transactionManager() default "";
//
//    @AliasFor(annotation = Transactional.class)
//    String[] label() default {};
//
//    @AliasFor(annotation = Transactional.class)
//    Propagation propagation() default Propagation.REQUIRED;
//
//    @AliasFor(annotation = Transactional.class)
//    Isolation isolation() default Isolation.DEFAULT;
//
//    @AliasFor(annotation = Transactional.class)
//    int timeout() default -1;
//
//    @AliasFor(annotation = Transactional.class)
//    String timeoutString() default "";
//
//    @AliasFor(annotation = Transactional.class)
//    boolean readOnly() default false;
//
//    @AliasFor(annotation = Transactional.class)
//    Class<? extends Throwable>[] rollbackFor() default {};
//
//    @AliasFor(annotation = Transactional.class)
//    String[] rollbackForClassName() default {};
//
//    @AliasFor(annotation = Transactional.class)
//    Class<? extends Throwable>[] noRollbackFor() default {};
//
//    @AliasFor(annotation = Transactional.class)
//    String[] noRollbackForClassName() default {};
//
//    /** 分布式锁的 key */
//    String key() default "";
//}
