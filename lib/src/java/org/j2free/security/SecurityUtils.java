/*
 * SecurityUtils.java
 *
 * Created on September 13, 2007, 2:35 PM
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Ryan Wilson
 */
public class SecurityUtils {

    /**
     *  Yea, this isn't the prettiest code, but compared to a version that uses
     *  Integer.toHexString() it's roughly two seconds faster at 1,000,000 reps
     * @param text
     * @return
     * @deprecated
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

    /**
     *
     * @param text
     * @return
     * @deprecated
     */
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
     *  @param o 
     * @return the MD5 hash of the object
     * @throws IOException
     */
    public static String MD5(Object o) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(o);

        return MD5(baos.toByteArray());
    }

    /**
     *  @param bytes
     * @return Calculates the MD5 hash of the byte array
     * @deprecated
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
