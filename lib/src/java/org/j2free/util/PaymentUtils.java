/*
 * PaymentUtils.java
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
package org.j2free.util;

/**
 *
 * @author Arjun Lall
 */
public class PaymentUtils {

    /**
     * 
     * @param s
     * @return
     */
    public static String getCreditCardDigitsOnly(String s)
    {
        StringBuffer digitsOnly = new StringBuffer();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (Character.isDigit(c)) {
                digitsOnly.append(c);
            }
        }
        return digitsOnly.toString();
    }

    //-------------------
    // Perform Luhn check
    //-------------------
    /**
     * 
     * @param cardNumber
     * @return
     */
    public static boolean isValidCreditCard(String cardNumber)
    {
        
        if (cardNumber == null || cardNumber.length() <= 0) {
            return false;
        }
        
        String digitsOnly = getCreditCardDigitsOnly(cardNumber);
        int sum = 0;
        int digit = 0;
        int addend = 0;
        boolean timesTwo = false;

        for (int i = digitsOnly.length() - 1; i >= 0; i--) {
            digit = Integer.parseInt(digitsOnly.substring(i, i + 1));
            if (timesTwo) {
                addend = digit * 2;
                if (addend > 9) {
                    addend -= 9;
                }
            } else {
                addend = digit;
            }
            sum += addend;
            timesTwo = !timesTwo;
        }

        int modulus = sum % 10;
        return modulus == 0;
    }
    
    /**
     * 
     * @param cardNumber
     * @return
     */
    public static String getCreditCardType(String cardNumber)
    {
        int firstDigit = Character.getNumericValue(cardNumber.charAt(0));
        switch(firstDigit) {
            case 3: return "Amex";
            case 4: return "Visa";
            case 5: return "MasterCard";
            case 6: return "Discover";
        }
        return "invalid";
    }
            
}
