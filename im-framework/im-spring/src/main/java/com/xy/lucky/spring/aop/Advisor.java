package com.xy.lucky.spring.aop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Advisor {
    private final String expression;
    private final String aspectBeanName;
    private final Supplier<Object> aspectSupplier;
    private final MethodHandle aspectHandle;
    private final boolean aspectStatic;

    private final Pattern classNamePattern;
    private final Pattern methodNamePattern;

    public Advisor(String expression, String aspectBeanName, Supplier<Object> aspectSupplier, Method aspectMethod) {
        this.expression = Objects.requireNonNull(expression, "expression");
        this.aspectBeanName = Objects.requireNonNull(aspectBeanName, "aspectBeanName");
        this.aspectSupplier = Objects.requireNonNull(aspectSupplier, "aspectSupplier");
        Objects.requireNonNull(aspectMethod, "aspectMethod");
        this.aspectStatic = java.lang.reflect.Modifier.isStatic(aspectMethod.getModifiers());
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(aspectMethod.getDeclaringClass(), lookup);
            MethodType type = MethodType.methodType(aspectMethod.getReturnType(), aspectMethod.getParameterTypes());
            this.aspectHandle = this.aspectStatic
                    ? privateLookup.findStatic(aspectMethod.getDeclaringClass(), aspectMethod.getName(), type)
                    : privateLookup.findVirtual(aspectMethod.getDeclaringClass(), aspectMethod.getName(), type);
        } catch (Exception e) {
            throw new IllegalStateException("构建切面方法句柄失败: " + aspectMethod.getDeclaringClass().getName() + "." + aspectMethod.getName(), e);
        }

        Pointcut pointcut = Pointcut.parse(expression);
        this.classNamePattern = pointcut.classNamePattern;
        this.methodNamePattern = pointcut.methodNamePattern;
    }

    public boolean matches(Class<?> targetClass, Method method) {
        return classNamePattern.matcher(targetClass.getName()).matches()
                && methodNamePattern.matcher(method.getName()).matches();
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object aspect = aspectSupplier.get();
        if (aspect == null) {
            throw new IllegalStateException("切面 Bean 不存在: " + aspectBeanName);
        }
        return aspectStatic ? aspectHandle.invoke(invocation) : aspectHandle.invoke(aspect, invocation);
    }

    private static final class Pointcut {
        private final Pattern classNamePattern;
        private final Pattern methodNamePattern;

        private Pointcut(Pattern classNamePattern, Pattern methodNamePattern) {
            this.classNamePattern = classNamePattern;
            this.methodNamePattern = methodNamePattern;
        }

        private static Pointcut parse(String expression) {
            String core = expression == null ? "" : expression.trim();

            if (core.startsWith("execution")) {
                int l = core.indexOf('(');
                int r = core.lastIndexOf(')');
                if (l >= 0 && r > l) {
                    core = core.substring(l + 1, r).trim();
                }
            }

            int space = core.indexOf(' ');
            if (space >= 0) {
                core = core.substring(space + 1).trim();
            }

            int paren = core.indexOf('(');
            if (paren >= 0) {
                core = core.substring(0, paren).trim();
            }

            int lastDot = core.lastIndexOf('.');
            if (lastDot < 0) {
                throw new IllegalArgumentException("不支持的 pointcut 表达式: " + expression);
            }

            String classPattern = core.substring(0, lastDot).trim();
            String methodPattern = core.substring(lastDot + 1).trim();

            Pattern cls = Pattern.compile("^" + toClassNameRegex(classPattern) + "$");
            Pattern mtd = Pattern.compile("^" + toMethodNameRegex(methodPattern) + "$");
            return new Pointcut(cls, mtd);
        }

        private static String toMethodNameRegex(String methodPattern) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < methodPattern.length(); i++) {
                char c = methodPattern.charAt(i);
                if (c == '*') {
                    sb.append(".*");
                } else {
                    sb.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return sb.toString();
        }

        private static String toClassNameRegex(String classPattern) {
            String p = classPattern.replace("..", "#DOTDOT#");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (c == '*') {
                    sb.append("[^\\.]*");
                } else if (c == '.') {
                    sb.append("\\.");
                } else if (c == '#') {
                    if (p.startsWith("#DOTDOT#", i)) {
                        sb.append("(\\.[^\\.]+)*");
                        i += "#DOTDOT#".length() - 1;
                    } else {
                        sb.append(Pattern.quote(String.valueOf(c)));
                    }
                } else {
                    sb.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return sb.toString();
        }
    }
}
