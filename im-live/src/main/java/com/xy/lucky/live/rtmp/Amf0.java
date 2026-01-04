package com.xy.lucky.live.rtmp;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简 AMF0 编解码，仅覆盖 RTMP 命令需要的字符串/数字/对象/ECMA 数组
 */
public final class Amf0 {
    private Amf0() {
    }

    public static Object read(ByteBuf in) {
        byte type = in.readByte();
        return switch (type) {
            case 0x02 -> readString(in);
            case 0x00 -> readNumber(in);
            case 0x03 -> readObject(in);
            case 0x08 -> readEcmaArray(in);
            default -> null;
        };
    }

    public static String readString(ByteBuf in) {
        int len = in.readUnsignedShort();
        byte[] b = new byte[len];
        in.readBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    public static Double readNumber(ByteBuf in) {
        long bits = in.readLong();
        return Double.longBitsToDouble(bits);
    }

    public static Map<String, Object> readObject(ByteBuf in) {
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            if (in.readableBytes() < 3) break;
            int keyLen = in.readUnsignedShort();
            if (keyLen == 0) {
                byte end = in.readByte();
                if (end == 0x09) break;
                else continue;
            }
            byte[] k = new byte[keyLen];
            in.readBytes(k);
            String key = new String(k, StandardCharsets.UTF_8);
            Object val = read(in);
            map.put(key, val);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readEcmaArray(ByteBuf in) {
        int count = in.readInt();
        Map<String, Object> map = new LinkedHashMap<>(count);
        while (true) {
            if (in.readableBytes() < 3) break;
            int keyLen = in.readUnsignedShort();
            if (keyLen == 0) {
                byte end = in.readByte();
                if (end == 0x09) break;
                else continue;
            }
            byte[] k = new byte[keyLen];
            in.readBytes(k);
            String key = new String(k, StandardCharsets.UTF_8);
            Object val = read(in);
            map.put(key, val);
        }
        return map;
    }
}

