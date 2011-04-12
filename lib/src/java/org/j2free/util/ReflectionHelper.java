/*
 * ReflectionHelper.java
 *
 * Created on October 2, 2008, 4:35 PM
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

package org.j2free.util;

/**
 * @author Ryan Wilson 
 */
public class ReflectionHelper {
    
    /**
     *  @param clazz
     * @param inter
     * @return true if Class inter is an interface, and the Class clazz
     *          implements Class inter, otherwise false
     */
    public static boolean implementsInterface(Class<?> clazz, Class inter)
    {
        if (!inter.isInterface())
            return false;
        
        return inter.isAssignableFrom(clazz);
    }
    
    /**
     *  @param clazz
     * @param extended
     * @return true if Class clazz extends Class extended
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