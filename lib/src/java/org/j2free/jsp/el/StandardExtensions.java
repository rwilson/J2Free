/*
 * StandardExtensions.java
 *
 * Created on March 21, 2008, 9:43 AM
 *
 */

package org.j2free.jsp.el;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import static org.j2free.etc.ServletUtils.*;

/**
 *
 * @author ryan
 */
public class StandardExtensions {
    
    public static boolean instanceOf(Object obj, String className) {
        return obj == null || className == null ? false :obj.getClass().getName().equals(className);
    }
    
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    public static String formatPercent(float d) {

        DecimalFormat n = new DecimalFormat();
        n.setMinimumFractionDigits(2);
        n.setMaximumFractionDigits(2);

        return n.format(d);
    }
    
    public static int integerDivision(int i0, int i1) {
        if (i1 > 0)
            return i0 / i1;
        
        return 0;
    }
    
    public static String escapeSingleQuotes(String text) {
        return text.replaceAll("'","\\\\'");
    }

    public static String escapeDoubleQuotes(String text) {
        return text.replaceAll("\"","\\\\\"");
    }

    public static String replaceAll(String text, String match, String replace) {
        return text.replaceAll(match,replace);
    }
    
    public static String formatDecimal(double decimal, int min, int max) {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(min);
        df.setMaximumFractionDigits(max);
        return df.format(decimal);
    }

    public static boolean isUserInRole(HttpServletRequest request, String role) {
        if (request == null || role == null)
            return false;
        
        return request.isUserInRole(role);
    }
    
    public static boolean hasValidURLExtension(String URL) {
        URL = URL.toLowerCase();
        URL = URL.replaceAll("^http://","");
        URL = URL.replaceAll("^www\\.","");
        
        if (!URL.contains("/")) {
            URL += '/';
        }
        
        String domain = URL.substring(0,URL.indexOf('/'));
        return domain.matches(".*?\\.([a-z]{2}|com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum)$");
    }
    
    public static boolean startsWithVowel(String string) {
        if (string == null)
            return false;
        
        char[] vowels = new char[]{
            'a','e','i','o','u','A','E','I','O','U'
        };
        return Arrays.binarySearch(vowels,string.charAt(0)) != -1;
    }
    
    public static int indexOf(String toFind, String toFindIn) {
        return toFindIn.indexOf(toFind);
    }
    
    public static int stringLength(String string) {
        return string.length();
    }
    
    public static String toLower(String s) {
        return s.toLowerCase();
    }
    
    public static String toUpper(String s) {
        return s.toUpperCase();
    }
    
    public static String trim(String s) {
        return s.trim();
    }
    
    public static String commify(int n) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        return nf.format(n);
    }
    
    public static String capitalizeFirst(String str) {
        if (empty(str))
            return str;
        
        if (str.length() == 1)
            return str.toUpperCase();
        
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }
    
    public static String cleanXSS(String str) {
        return str.replace("<","&lt;")
                  .replace(">","&gt;")
                  .replace("&","&amp;")
                  .replace("'","&#39")
                  .replace("\"","&quot;");
    }
}