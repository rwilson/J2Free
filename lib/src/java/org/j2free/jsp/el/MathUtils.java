package org.j2free.jsp.el;

/**
 *
 * @author Ryan Wilson
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

    public static double max(double... doubles) {
        double max = -1d;

        for (double d : doubles) {
            if (max == -1d || d > max) {
                max = d;
            }
        }

        return max;
    }

    public static double min(double... doubles) {
        double min = -1d;

        for (double d : doubles) {
            if (min == -1d || d < min) {
                min = d;
            }
        }

        return min;
    }
}
