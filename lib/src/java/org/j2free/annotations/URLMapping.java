/*
 * URLMapping.java
 *
 * Created on April 8, 2008, 2:49 AM
 *
 * Modified May 5, 2009 to include regex property.
 */

package org.j2free.annotations;
import java.lang.annotation.*; 

/**
 *
 * @author Arjun
 * @author Ryan
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface URLMapping {
    public String[] urls() default {};
    public String regex() default "";
    public boolean ssl() default false;
}
