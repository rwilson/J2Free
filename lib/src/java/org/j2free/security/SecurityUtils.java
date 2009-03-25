/*
 * SecurityUtils.java
 *
 * Created on September 13, 2007, 2:35 PM
 *
 */

package org.j2free.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author ryan
 */
public class SecurityUtils {
    /*
    public static void main(String[] args) {
        
        long c = 0;
        
        try {
            
            File mp3 = new File("/guitar-files/carry_on.mp3");
            
            FileInputStream stream = new FileInputStream(mp3);
            
            long length = mp3.length();
            
            // Cannot create an array using a long type. It needs to be an int type.
            // Check to ensure that file is not larger than Integer.MAX_VALUE
            if (length > Integer.MAX_VALUE) {
                System.out.println("File too large... shit");
                return;
            }
            
            byte[] bytes = new byte[(int)length];
            
            // Read the bytes
            int offset  = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = stream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            
            // Ensure all bytes were read
            if (offset < bytes.length) {
                throw new IOException("Could not read whole file " + mp3.getName());
            }
            
            stream.close();
            
            String hash = "", lastHash = "";
            
            for (int i = 0; i < 1000; i++) {
                
                long s = System.currentTimeMillis();
                hash = MD5(bytes);
                long e = System.currentTimeMillis();

                if (i > 0 && !hash.equals(lastHash)) {
                    System.out.println("Hashes don't match!");
                    return;
                }
                
                lastHash = hash;
                
                c += (e - s);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Avg time: " + (c / 1000) + "ms");
    }
     */
        
    /**
     *  Yea, this isn't the prettiest code, but compared to a version that uses
     *  Integer.toHexString() it's roughly two seconds faster at 1,000,000 reps
     */
    private static String convertToHex(byte[] digest) {
        StringBuffer hex = new StringBuffer();
        
        for (int i = 0; i < digest.length; i++) {
            int halfbyte = (digest[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    hex.append((char) ('0' + halfbyte));
                else
                    hex.append((char) ('a' + (halfbyte - 10)));
                halfbyte = digest[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return hex.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, 
                                                  UnsupportedEncodingException {
        
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        return convertToHex(md.digest());
    }
    
    public static String MD5(String text) throws NoSuchAlgorithmException, 
                                                  UnsupportedEncodingException {
        
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        return convertToHex(md.digest());
    }
    
    /**
     *  Convenience method for MD5(byte[]) which uses ObjectOutputStream to write
     *  an object to a ByteArrayOutputStream to get the object as a byte[]
     *
     *  @return the MD5 hash of the object
     */
    public static String MD5(Object o) throws NoSuchAlgorithmException, 
                                              UnsupportedEncodingException, 
                                              IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos     = new ObjectOutputStream(baos);
        
        oos.writeObject(o);
        
        return MD5(baos.toByteArray());
    }
    
    /**
     *  @return Calculates the MD5 hash of the byte array
     */
    public static String MD5(byte[] bytes) throws NoSuchAlgorithmException, 
                                              UnsupportedEncodingException, 
                                              IOException {
        
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        return convertToHex(md.digest());
    }
}