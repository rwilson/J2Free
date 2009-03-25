/*
 * URLMapping.java
 *
 * Created on April 8, 2008, 2:49 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.j2free.annotations;
import java.lang.annotation.*; 

/**
 *
 * @author Arjun
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface URLMapping {
    public String[] urls();
    //public String regexUrl() default "";
}
