/*
 * SecurityUtils.java
 *
 * Created on September 13, 2007, 2:35 PM
 *
 */
package org.j2free.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author ryan
 */
public class SecurityUtils {

    /**
     *  Yea, this isn't the prettiest code, but compared to a version that uses
     *  Integer.toHexString() it's roughly two seconds faster at 1,000,000 reps
     */
//    private static String convertToHex(byte[] digest) {
//        StringBuffer hex = new StringBuffer();
//
//        for (int i = 0; i < digest.length; i++) {
//            int halfbyte = (digest[i] >>> 4) & 0x0F;
//            int two_halfs = 0;
//            do {
//                if ((0 <= halfbyte) && (halfbyte <= 9)) {
//                    hex.append((char) ('0' + halfbyte));
//                } else {
//                    hex.append((char) ('a' + (halfbyte - 10)));
//                }
//                halfbyte = digest[i] & 0x0F;
//            } while (two_halfs++ < 1);
//        }
//        return hex.toString();
//    }

    @Deprecated
    public static String SHA1(String text) {
        return DigestUtils.shaHex(text);
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-1");
//            md.update(text.getBytes("iso-8859-1"), 0, text.length());
//            return convertToHex(md.digest());
//        } catch (Exception e) {
//            throw LaunderThrowable.launderThrowable(e);
//        }
    }

    @Deprecated
    public static String MD5(String text) {
        return DigestUtils.md5Hex(text);
//        try {
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            md.update(text.getBytes("iso-8859-1"), 0, text.length());
//            return convertToHex(md.digest());
//        } catch (Exception e) {
//            throw LaunderThrowable.launderThrowable(e);
//        }
    }

    /**
     *  Convenience method for MD5(byte[]) which uses ObjectOutputStream to write
     *  an object to a ByteArrayOutputStream to get the object as a byte[]
     *
     *  @return the MD5 hash of the object
     */
    public static String MD5(Object o) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(o);

        return MD5(baos.toByteArray());
    }

    /**
     *  @return Calculates the MD5 hash of the byte array
     */
    @Deprecated
    public static String MD5(byte[] bytes) {
        return DigestUtils.md5Hex(bytes);
//        try {
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            md.update(bytes);
//            return convertToHex(md.digest());
//        } catch (Exception e) {
//            throw LaunderThrowable.launderThrowable(e);
//        }
    }
}
