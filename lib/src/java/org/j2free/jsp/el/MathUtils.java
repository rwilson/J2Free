/**
 * MathUtils.java
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

/**
 * @author Ryan Wilson
 */
public class MathUtils {

    /**
     * 
     * @param d
     * @return
     */
    public static int floor(double d)
    {
        return Double.valueOf(Math.floor(d)).intValue();
    }

    /**
     * 
     * @param d
     * @return
     */
    public static int ceiling(double d)
    {
        return Double.valueOf(Math.ceil(d)).intValue();
    }

    /**
     * 
     * @param one
     * @param two
     * @return
     */
    public static double max(double one, double two)
    {
        return Math.max(one, two);
    }

    /**
     * 
     * @param one
     * @param two
     * @return
     */
    public static double min(double one, double two)
    {
        return Math.min(one,two);
    }

    /**
     * 
     * @param doubles
     * @return
     */
    public static double max(double... doubles)
    {
        double max = -1d;

        for (double d : doubles) {
            if (max == -1d || d > max) {
                max = d;
            }
        }

        return max;
    }

    /**
     * 
     * @param doubles
     * @return
     */
    public static double min(double... doubles)
    {
        double min = -1d;

        for (double d : doubles) {
            if (min == -1d || d < min) {
                min = d;
            }
        }

        return min;
    }
}
