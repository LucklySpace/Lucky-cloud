package com.xy.lucky.connect.domain;


import java.io.Serializable;
import java.util.Objects;


public class ImPair<K, V> implements Serializable {
    private static final long serialVersionUID = 1L;
    protected K key;
    protected V value;

    public ImPair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> ImPair<K, V> of(K key, V value) {
        return new ImPair<K, V>(key, value);
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public String toString() {
        return "Pair [key=" + this.key + ", value=" + this.value + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ImPair)) {
            return false;
        } else {
            ImPair<?, ?> pair = (ImPair) o;
            return Objects.equals(this.getKey(), pair.getKey()) && Objects.equals(this.getValue(), pair.getValue());
        }
    }

    public int hashCode() {
        return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
    }
}
