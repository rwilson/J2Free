/*
 * StandardExtensions.java
 *
 * Created on March 21, 2008, 9:43 AM
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
package org.j2free.jsp.el;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

import org.j2free.util.ServletUtils;

/**
 * @author Ryan Wilson
 */
public class StandardExtensions
{
    /**
     *
     * @param obj
     * @param className
     * @return
     */
    public static boolean instanceOf(Object obj, String className)
    {
        return obj == null || className == null ? false :obj.getClass().getName().equals(className);
    }
    
    /**
     *
     * @return
     */
    public static long currentTimeMillis()
    {
        return System.currentTimeMillis();
    }
    
    /**
     *
     * @param d
     * @return
     */
    public static String formatPercent(float d)
    {

        DecimalFormat n = new DecimalFormat();
        n.setMinimumFractionDigits(2);
        n.setMaximumFractionDigits(2);
        return n.format(d);
    }
    
    /**
     *
     * @param i0
     * @param i1
     * @return
     */
    public static int integerDivision(int i0, int i1)
    {
        if (i1 > 0)
            return i0 / i1;
        
        return 0;
    }
    
    /**
     *
     * @param text
     * @return
     */
    public static String escapeSingleQuotes(String text)
    {
        return ServletUtils.escapeSingleQuotes(text);
    }

    /**
     *
     * @param text
     * @return
     */
    public static String escapeDoubleQuotes(String text)
    {
        return ServletUtils.escapeDoubleQuotes(text);
    }

    /**
     *
     * @param text
     * @param match
     * @param replace
     * @return
     */
    public static String replaceAll(String text, String match, String replace)
    {
        return text.replaceAll(match,replace);
    }
    
    /**
     *
     * @param decimal
     * @param min
     * @param max
     * @return
     */
    public static String formatDecimal(double decimal, int min, int max)
    {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(min);
        df.setMaximumFractionDigits(max);
        return df.format(decimal);
    }

    /**
     *
     * @param request
     * @param role
     * @return
     */
    public static boolean isUserInRole(HttpServletRequest request, String role)
    {
        if (request == null || role == null)
            return false;
        
        return request.isUserInRole(role);
    }
    
    /**
     *
     * @param URL
     * @return
     */
    public static boolean hasValidURLExtension(String URL)
    {
        URL = URL.toLowerCase();
        URL = URL.replaceAll("^http://","");
        URL = URL.replaceAll("^www\\.","");
        
        if (!URL.contains("/")) {
            URL += '/';
        }
        
        String domain = URL.substring(0,URL.indexOf('/'));
        return domain.matches(".*?\\.([a-z]{2}|com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum)$");
    }
    
    /**
     *
     * @param string
     * @return
     */
    public static boolean startsWithVowel(String string)
    {
        if (string == null)
            return false;
        
        char[] vowels = new char[]{
            'a','e','i','o','u','A','E','I','O','U'
        };
        return Arrays.binarySearch(vowels,string.charAt(0)) != -1;
    }
    
    /**
     *
     * @param toFind
     * @param toFindIn
     * @return
     */
    public static int indexOf(String toFind, String toFindIn)
    {
        return toFindIn.indexOf(toFind);
    }
    
    /**
     *
     * @param string
     * @return
     */
    public static int stringLength(String string)
    {
        return string.length();
    }
    
    /**
     *
     * @param s
     * @return
     */
    public static String toLower(String s)
    {
        return s.toLowerCase();
    }
    
    /**
     *
     * @param s
     * @return
     */
    public static String toUpper(String s)
    {
        return s.toUpperCase();
    }
    
    /**
     *
     * @param s
     * @return
     */
    public static String trim(String s)
    {
        return s.trim();
    }
    
    /**
     *
     * @param n
     * @return
     */
    public static String commify(int n)
    {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        return nf.format(n);
    }
    
    /**
     *
     * @param str
     * @return
     */
    public static String capitalizeFirst(String str)
    {
        return ServletUtils.capitalizeFirst(str);
    }
    
    /**
     *
     * @param str
     * @return
     */
    public static String urlEncode(String str)
    {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }
    
    /**
     *
     * @param str
     * @return
     */
    public static String cleanXSS(String str)
    {
        return ServletUtils.cleanXSS(str);
    }
 
    /**
     * Proxy for {@link String} <tt>matches</tt>.
     * 
     * @param toMatch The string to test.
     * @param regex The regex to look for.
     * @return <pre>toMatch.matches(regex)</pre>
     */
    public static boolean matches(String toMatch, String regex)
    {
        return toMatch.matches(regex);
    }

    /**
     *
     * @param request
     * @return
     */
    public static boolean isSecureRequest(HttpServletRequest request)
    {
        return request.isSecure();
    }


    /**
     * 
     * @param str
     * @return
     */
    public static String sha1Hash(String str)
    {
        return org.j2free.security.SecurityUtils.SHA1(str);
    }

    /**
     * 
     * @return
     */
    public static double random()
    {
        return Math.random();
    }

    /**
     * Adds the correct protocol.
     * @param request
     * @param url
     * @return
     */
    public static String addProtocol(HttpServletRequest request, String url)
    {
        if (isSecureRequest(request))
            return "https://" + url;
        else
            return "http://" + url;
    }

    /**
     * Converts the given ip from x.x.x.x to a number
     * @param addr
     * @return
     */
    public static int ipToInt(String addr)
    {
        if (StringUtils.isBlank(addr))
            return 0;

        String[] bytes = addr.split("\\.");
        int ip = 0;
        for (String s : bytes) {
            ip <<= 8;
            ip |= Integer.parseInt(s);
        }
        return ip;
    }

    /**
     * Converts the given int to an ip addr of the form x.x.x.x
     * @param i 
     * @return
     */
    public static String intToIp(int i)
    {
        return ((i >> 24) & 0xFF) + "." +
               ((i >> 16) & 0xFF) + "." +
               ((i >>  8) & 0xFF) + "." +
               ( i        & 0xFF);
    }
}