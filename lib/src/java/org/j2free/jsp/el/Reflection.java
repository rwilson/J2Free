/*
 * Reflection.java
 *
 * Created on June 27, 2008, 6:39 PM
 *
 * Copyright (c) 2008 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.j2free.jsp.el;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Entity;
import org.j2free.admin.ReflectionMarshaller;

/**
 * @author Ryan Wilson 
 */
public class Reflection {
    
    public static HashMap<String,Object> constantsMap = new HashMap<String, Object>();
    
    public static boolean isEntity(Object obj) {
        return obj.getClass().getAnnotation(Entity.class) != null;
    }
    
    public static String extractId(Object obj) {
        Class klass = obj.getClass();
        if (klass.getAnnotation(Entity.class) == null)
            return null;
        
        ReflectionMarshaller marshaller = ReflectionMarshaller.getForClass(klass);
        if (marshaller == null)
            return "unknown";
        else
            return "" + marshaller.extractId(obj);
    }
    
    public static Object getStaticConstant(String className, String property) {
        if (constantsMap.containsKey(className+"."+property)) {
            return constantsMap.get(className+"."+property);
        }
        try {
            Class klass = Class.forName(className);
            Field fld = klass.getField(property);
            // Obj parameter can be null since field is static
            Object o = fld.get(null);
            
            constantsMap.put(className+"."+property, o);
            
            return o;
            
        } catch (Exception ex) {
            Logger.getLogger(Reflection.class.getName()).log(Level.INFO, null, ex);
        }
        
        return null;
    }
}
