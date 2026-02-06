package com.xy.lucky.lbs.utils;

/**
 * GeoHash工具类
 */
public class GeoHashUtil {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int[] BITS = {16, 8, 4, 2, 1};

    public static String encode(double lat, double lon) {
        return encode(lat, lon, 12);
    }

    public static String encode(double lat, double lon, int precision) {
        StringBuilder geohash = new StringBuilder();
        boolean isEven = true;
        int bit = 0;
        int ch = 0;
        double[] latInterval = {-90.0, 90.0};
        double[] lonInterval = {-180.0, 180.0};

        while (geohash.length() < precision) {
            double mid = 0.0;
            if (isEven) {
                mid = (lonInterval[0] + lonInterval[1]) / 2;
                if (lon > mid) {
                    ch |= BITS[bit];
                    lonInterval[0] = mid;
                } else {
                    lonInterval[1] = mid;
                }
            } else {
                mid = (latInterval[0] + latInterval[1]) / 2;
                if (lat > mid) {
                    ch |= BITS[bit];
                    latInterval[0] = mid;
                } else {
                    latInterval[1] = mid;
                }
            }
            isEven = !isEven;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }

}
