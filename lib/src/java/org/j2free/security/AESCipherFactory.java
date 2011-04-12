/*
 * DESEncrypter.java
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


import javax.crypto.Cipher;

import javax.crypto.spec.SecretKeySpec;
import net.jcip.annotations.Immutable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.j2free.util.LaunderThrowable;

/**
 * Utility class for encrypting via a passphrase
 *
 * @author Arjun Lall
 */
@Immutable
public class AESCipherFactory
{
    /**
     * 
     * @param passPhrase
     * @param cipherMode
     * @return
     */
    public Cipher getCipher(byte[] passPhrase, int cipherMode)
    {
        
        Cipher cipher = null;
        
        try {
            // Create the key based on the passPhrase
            SecretKeySpec keySpec = new SecretKeySpec(passPhrase,"AES");
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            // Create the ciphers
            cipher.init(cipherMode, keySpec);

        } catch (Exception e) {
            throw LaunderThrowable.launderThrowable(e);
        }
        
        return cipher;
    }
    
    /**
     * 
     * @param key
     * @param cipherMode
     * @return
     */
    public Cipher getCipherFromHexString(String key, int cipherMode)
    {
        try {
            return getCipher(Hex.decodeHex(key.toCharArray()), cipherMode);
        } catch (DecoderException ex) {
            throw LaunderThrowable.launderThrowable(ex);
        }
    }
}
