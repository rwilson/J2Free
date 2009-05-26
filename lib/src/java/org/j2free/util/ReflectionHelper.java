/*
 * ReflectionHelper.java
 *
 * Created on October 2, 2008, 4:35 PM
 *
 * Copyright (c) 2008 FooBrew, Inc.
 */

package org.j2free.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryan Wilson 
 */
public class ReflectionHelper {
    
    /**
     *  @return true if Class inter is an interface, and the Class clazz
     *          implements Class inter, otherwise false
     */
    public static boolean implementsInterface(Class<?> clazz, Class inter) {
        
        if (!inter.isInterface())
            return false;
        
        return inter.isAssignableFrom(clazz);
    }
    
    /**
     *  @return true if Class clazz extends Class extended
     */
    public static boolean extendsClass(Class clazz, Class extended) {
        
        Class zuper = clazz;
        while ((zuper = zuper.getSuperclass()) != null)
            if (zuper.equals(extended))
                return true;
        
        return false;
    }
    
    
    public static void main(String[] args) {
        System.out.println("AddCountingSet implements Set: " + implementsInterface(AddCountingSet.class,Set.class));
        System.out.println("AddCountingSet implements Collection: " + implementsInterface(AddCountingSet.class,Collection.class));
        System.out.println("AddCountingSet implements Map: " + implementsInterface(AddCountingSet.class,Map.class));
        System.out.println("AddCountingSet extends AbstractSet: " + extendsClass(AddCountingSet.class,AbstractSet.class));
        System.out.println("AddCountingSet extends AbstractMap: " + extendsClass(AddCountingSet.class,AbstractMap.class));
    }
}