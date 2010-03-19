/*
 * ReflectionHelper.java
 *
 * Created on October 2, 2008, 4:35 PM
 *
 * Copyright (c) 2008 FooBrew, Inc.
 */

package org.j2free.util;

/**
 * @author Ryan Wilson 
 */
public class ReflectionHelper {
    
    /**
     *  @return true if Class inter is an interface, and the Class clazz
     *          implements Class inter, otherwise false
     */
    public static boolean implementsInterface(Class<?> clazz, Class inter)
    {
        if (!inter.isInterface())
            return false;
        
        return inter.isAssignableFrom(clazz);
    }
    
    /**
     *  @return true if Class clazz extends Class extended
     */
    public static boolean extendsClass(Class clazz, Class extended)
    {
        Class zuper = clazz;
        while ((zuper = zuper.getSuperclass()) != null)
            if (zuper.equals(extended))
                return true;
        
        return false;
    }    
}