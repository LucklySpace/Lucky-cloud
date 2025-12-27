package com.xy.lucky.utils.collection;


import java.util.Set;

/**
 * Set 工具类
 */
public class SetUtils {

    @SafeVarargs
    public static <T> Set<T> asSet(T... objs) {
        return CollectionUtils.newHashSet(objs);
    }

}
