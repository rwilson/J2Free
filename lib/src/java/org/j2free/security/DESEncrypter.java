/*
 * DESEncrypter.java
 *
 * Copyright (c) 2011 FooBrew, Inc.
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

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import net.jcip.annotations.Immutable;

import org.apache.commons.codec.binary.Base64;
import org.j2free.util.LaunderThrowable;

/**
 * Utility class for encrypting via a passphrase
 *
 * @author Ryan Wilson
 */
@Immutable
public class DESEncrypter {

    private final byte[] salt = {
        (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
        (byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
    };
    private final int ITERATION_COUNT = 19;
    
    private final Cipher encoder;
    private final Cipher decoder;

    /**
     * 
     * @param passPhrase
     */
    public DESEncrypter(String passPhrase)
    {

        try {
            // Create the key based on the passPhrase
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, ITERATION_COUNT);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

            encoder = Cipher.getInstance(key.getAlgorithm());
            decoder = Cipher.getInstance(key.getAlgorithm());

            // Prepare the parameter to the ciphers
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, ITERATION_COUNT);

            // Create the ciphers
            encoder.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            decoder.init(Cipher.DECRYPT_MODE, key, paramSpec);

        } catch (Exception e) {
            throw LaunderThrowable.launderThrowable(e);
        }
    }

    /**
     * @param str A <tt>String</tt> to encrypt
     * @return A BASE64 encoded, encrypted <tt>String</tt>
     */
    public synchronized String encrypt(String str) {
        try {

            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes("UTF8");

            // Encrypt
            byte[] enc = encoder.doFinal(utf8);

            // Encode bytes to base64 to get a string
            return new String(Base64.encodeBase64(enc,false));
            
        } catch (Exception e) {
            throw LaunderThrowable.launderThrowable(e);
        }
    }

    /**
     * @param str A BASE64 encoded, encrypted <tt>String</tt>
     * @return The decrypted <tt>String</tt>
     */
    public synchronized String decrypt(String str) {
        try {

            // Decode base64 to get bytes
            byte[] dec = Base64.decodeBase64(str.getBytes("UTF8"));

            // Decrypt
            byte[] utf8 = decoder.doFinal(dec);

            // Decode using utf-8
            return new String(utf8, "UTF8");
            
        } catch (Exception e) {
            throw LaunderThrowable.launderThrowable(e);
        }
    }

    /**
     * 
     * @return
     */
    public Cipher getDecoder()
    {
        return decoder;
    }

    /**
     * 
     * @return
     */
    public Cipher getEncoder()
    {
        return encoder;
    }
}
