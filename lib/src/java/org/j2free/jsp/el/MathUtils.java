/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.j2free.jsp.el;

/**
 *
 * @author ryan
 */
public class MathUtils {

    public static int floor(double d) {
        return Double.valueOf(Math.floor(d)).intValue();
    }

    public static int ceiling(double d) {
        return Double.valueOf(Math.ceil(d)).intValue();
    }

    public static double max(double one, double two) {
        return Math.max(one, two);
    }

    public static double min(double one, double two) {
        return Math.min(one,two);
    }
}
