/*
 * GChartUtil.java
 *
 * Created on June 27, 2009
 *
 */

package org.j2free.util;

/**
 * Utility class for working with GChart
 *
 * @author Ryan Wilson
 */
public class GChartUtil {
    
    private static final String SIMPLE_MAP   = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String EXTENDED_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.";

    public static String encodeSimple(int num) {
        if (num < 0 || num > SIMPLE_MAP.length() - 1) {
            return "";
        }
        return "" + SIMPLE_MAP.charAt(num);
    }

    /**
     * Encodes an int in gchart extended encoding
     * data format.
     *
     * @param num the int to encode
     * @return the int in gchart extended encoding
     */
    public static String encodeExtended(int num) {
        if (num < 0 || num > EXTENDED_MAP.length() * EXTENDED_MAP.length() - 1) {
            return "";
        }

        int quotient  = num / EXTENDED_MAP.length();
        int remainder = num - EXTENDED_MAP.length() * quotient;

        return EXTENDED_MAP.charAt(quotient) + "" + EXTENDED_MAP.charAt(remainder);
    }

    /**
     *
     * @param ints an array of ints
     * @return A String containing the ints data in gchart
     *         extended encoding
     */
    public static String encodeExtended(int[] ints) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < ints.length; i++) {
            str.append(GChartUtil.encodeExtended(ints[i]));
        }
        return str.toString();
    }

    /**
     * Scales the data to fit within the 4095 limit of
     * gchart extended encoding.
     *
     * @param ints the array to scale
     */
    public static void scaleForExtendedEncoding(int[] ints) {

        int min = -1,
            max = -1;
        
        for (int i = 0; i < ints.length; i++) {
            if (min == -1 || ints[i] < min) {
                min = ints[i];
            } 
            
            if (max == -1 || ints[i] > max) {
                max = ints[i];
            }
        }
        
        scaleForExtendedEncoding(ints, min, max);
    }

    public static void scaleForExtendedEncoding(int[] ints, int min, int max) {
        double scale = 4095.0d / (double)(max - min);

        for (int i = 0; i < ints.length; i++) {
            ints[i] = ((Double)(ints[i] * scale)).intValue();
        }
    }

    /**
     * Scales and encodes the array of ints using the
     * min and max in this array for scaling.
     */
    public static String scaleAndEncode(int[] ints) {
        scaleForExtendedEncoding(ints);
        return encodeExtended(ints);
    }

    /**
     * Scales and encodes the array of ints using the
     * min and max arguments for scaling.  USE THIS
     * METHOD WHEN ENCODING MULTIPLE DATA SETS TO BE
     * DISPLAYED ON THE SAME GRAPH.
     */
    public static String scaleAndEncode(int[] ints, int min, int max) {
        scaleForExtendedEncoding(ints, min, max);
        return encodeExtended(ints);
    }

}