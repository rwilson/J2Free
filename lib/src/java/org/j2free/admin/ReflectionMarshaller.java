/*
 * ReflectionMarshaller.java
 *
 * Created on April 3, 2008, 1:58 PM
 *
 * Copyright (c) 2008 Publi.us
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

package org.j2free.admin;

import org.j2free.util.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.jpa.Controller;

/**
 *  Marshalls Objects of unknown Types in order to access their fields and
 *  methods;
 *
 * @author Ryan Wilson 
 */
public final class ReflectionMarshaller implements Marshaller {
    
    private static final Log log = LogFactory.getLog(ReflectionMarshaller.class);
    
    /**************************************************************************/
    // Static Implementation
    /**************************************************************************/
    
    /* Maps Class names to previously generated Marshallers so we don't have to
     * go through the expensive process of creating the same marshaller twice.
     */
    protected static ConcurrentHashMap<String,ReflectionMarshaller> marshallers = new ConcurrentHashMap();
    
    public static ReflectionMarshaller getForClass(Class klass) {
        
        ReflectionMarshaller marshaller = marshallers.get(klass.getName());
        
        if (marshaller != null)
            return marshaller;
        
        if (marshaller == null) {
            try {
                marshaller = new ReflectionMarshaller(klass);
            } catch (Exception e) {
                log.error("Error creating marshaller for " + klass.getName(),e);
                return null;
            }
        }
        
        marshallers.put(klass.getName(),marshaller);
        return marshaller;
    }
    
    
    /**************************************************************************/
    // Instance Implementation
    /**************************************************************************/
    
    /*  Improvements to make
     *    - allow editing of transient fields, but also allow filtering to
     *      only pay attention to fields with @Column
     *    - Allow customization of how displayed values are rendered and how
     *      lists are sorted via annotations
     *    - Allow customization of displayed value in lists via web-interface
     *      i.e. Let users select which field is the identifying field
     *    - Make display column width resizable
     *    - History management crap
     */
    
    protected Class klass;
    
    protected HashMap<Field, Converter> instructions;
    
    protected Field entityIdField;
    
    protected boolean embeddedId;
    
    private ReflectionMarshaller(Class klass) throws Exception {

        instructions = new HashMap<Field, Converter>();
        
        LinkedList<Field> fieldsToMarshall = new LinkedList<Field>();
        
        // Only marshallOut entities
        if (!klass.isAnnotationPresent(Entity.class))
            throw new Exception("Provided class is not an @Entity");
        
        // Add the declared fields
        fieldsToMarshall.addAll(Arrays.asList(klass.getDeclaredFields()));
        
        /* Inheritence support
         * Continue up the inheritance ladder until:
         *   - There are no more super classes (zuper == null), or
         *   - The super class is not an @Entity
         */
        Class zuper = klass;
        while ((zuper = zuper.getSuperclass()) != null) {
            
            // get out if we find a super class that isn't an @Entity
            if (!klass.isAnnotationPresent(Entity.class))
                break;
            
            // Add the declared fields
            // @todo, improve the inheritance support, the current way will overwrite
            // overridden fields in subclasses with the super class's field
            fieldsToMarshall.addAll(Arrays.asList(zuper.getDeclaredFields()));
        }
        
        /* By now, fieldsToMarshall should contain all the fields
         * so it's time to figure out how to access them.
         */
        Method getter, setter;
        Converter converter;
        for (Field field : fieldsToMarshall) {
            
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                log.debug("Skipping final or static field " + field.getName());
                continue;
            }
            
            getter = setter = null;
            
            // if direct access doesn't work, look for JavaBean
            // getters and setters
            String fieldName  = field.getName();
            Class  fieldType  = field.getType();
            
            try {
                getter = getGetter(field);
            } catch (NoSuchMethodException nsme) {
                log.debug("Failed to find getter for " + fieldName);
            }
            
            try {
                setter = getSetter(field);
            } catch (NoSuchMethodException nsme) {
                log.debug("Failed to find setter for " + fieldName);
            }
            
            if (getter == null && setter == null) {
                // Shit, we didn't figure out how to access it
                log.debug("Could not access field: " + field.getName());
            } else {
                converter = new Converter(getter,setter);
                
                if (field.isAnnotationPresent(Id.class)) {
                    log.debug("Found entityIdFied for " + klass.getName() + ": " + field.getName());
                    entityIdField = field;
                    embeddedId    = false;
                }
                
                if (field.isAnnotationPresent(EmbeddedId.class)) {
                    log.debug("Found embedded entityIdFied for " + klass.getName() + ": " + field.getName());
                    entityIdField = field;
                    embeddedId    = true;
                }
                
                if (field.isAnnotationPresent(GeneratedValue.class) || setter == null) {
                    converter.setReadOnly(true);
                }
                
                if (field.getType().isAnnotationPresent(Entity.class)) {
                    converter.setEntity(fieldType);
                }
                
                Class superClass = field.getType();
                if (superClass != null) {
                    do {
                        if (superClass == Collection.class) {
                            try {
                                Type type = field.getGenericType();
                                String typeString = type.toString();

                                while (typeString.matches("[^<]+?<[^>]+?>"))
                                    typeString = typeString.substring(typeString.indexOf("<") + 1, typeString.indexOf(">"));

                                Class collectionType = Class.forName(typeString);
                                converter.setCollection(collectionType);

                                if (collectionType.getAnnotation(Entity.class) != null)
                                    converter.setEntity(collectionType);

                                log.debug(field.getName() + " is entity = " + converter.isEntity());
                                log.debug(field.getName() + " collectionType = " + converter.getType().getSimpleName());

                            } catch (Exception e) {
                                log.debug("error getting collection type",e);
                            } finally {
                                break;
                            }
                        }
                        superClass = superClass.getSuperclass();
                    } while (superClass != null);
                }
                
                instructions.put(field,converter);
            }
        }
    }
    
    public Object extractId(Object obj) {
        if (entityIdField == null)
            return null;
        
        if (!entityIdField.isAccessible())
            entityIdField.setAccessible(true);
        
        try {
            
            return entityIdField.get(obj);
            
        } catch (IllegalAccessException iae) {
            try {
                Method idGetter = getGetter(entityIdField);
                
                if (!idGetter.isAccessible())
                    idGetter.setAccessible(true);
                
                if (!idGetter.isAccessible())
                    return null;
                
                return idGetter.invoke(obj);
                
            } catch (NoSuchMethodException nsme) {
                log.error(nsme);
            } catch (IllegalAccessException ia) {
                log.error(ia);
            } catch (InvocationTargetException ite) {
                log.error(ite);
            }
        }
        return null;
    }
 
    public Method getGetter(Field field) throws NoSuchMethodException {
        if (isBoolean(field.getType()))
            return field.getDeclaringClass().getMethod("is" + capitalizeIndex(field.getName(),0));
        else
            return field.getDeclaringClass().getMethod("get" + capitalizeIndex(field.getName(),0));
    }
    
    public Method getSetter(Field field) throws NoSuchMethodException {
        return field.getDeclaringClass().getMethod("set" + capitalizeIndex(field.getName(),0),field.getType());
    }
    
    public List<MarshalledField> marshallOut(Object entity, boolean includeTransient) throws IllegalArgumentException {
        
        TreeSet<MarshalledField> fields = new TreeSet<MarshalledField>();
        
        if (entity == null) {
            for (Map.Entry<Field,Converter> ent : instructions.entrySet()) {
                fields.add(
                        new MarshalledField(ent.getKey().getName())
                    );
            }
            return new LinkedList<MarshalledField>(fields);
        }
        
        log.debug("Marshalling out instance of " + entity.getClass().getSimpleName());
        
        Field     field;
        Converter converter;
        Object    value;
        Method    getter;
        
        boolean error = false;
        
        for (Map.Entry<Field,Converter> ent : instructions.entrySet()) {
            
            error = false;
            
            field     = ent.getKey();
            converter = ent.getValue();
            value     = null;
            
            log.debug("Converting field " + field.getName());
            
            if ((field.isAnnotationPresent(Transient.class) || Modifier.isTransient(field.getModifiers())) && !includeTransient)
                continue;
            
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (field.isAccessible()) {
                    value = convertNull(field.get(entity));
                } else {
                    error = true;
                }
            } catch (IllegalAccessException iae) {
                log.error("Unable to access " + entity.getClass().getSimpleName() + "." + field.getName() + " directly.",iae);
                error = true;
            }
            
            if (error) {
                error = false;
                try {
                    getter = converter.getGetter();
                    if (getter != null) {
                        if (!getter.isAccessible()) {
                            getter.setAccessible(true);
                        }
                        if (getter.isAccessible()) {
                            value = convertNull(getter.invoke(entity));
                        } else {
                            error = true;
                        }
                    } else {
                        error = true;
                    }
                } catch (IllegalAccessException iae) {
                    log.error("Error accessing getter for field " + entity.getClass().getSimpleName() + "." + field.getName(), iae);
                    error = true;
                } catch (InvocationTargetException ite) {
                    log.error("Error invoking getter for field " + entity.getClass().getSimpleName() + "." + field.getName(), ite);
                    error = true;
                }
            }
            
            if (error) {
                // if there was an error, then we weren't able to access the field,
                // so set the value to be displayed to "Inaccessible!" and override
                // the converter readonly value to make sure the display doesn't let
                // the user edit an inaccessible field.
                converter.setReadOnly(true);
                fields.add(new MarshalledField(field.getName(),"Inaccessible!",converter));
            } else {
                fields.add(new MarshalledField(field.getName(),value,converter));
            }
        }
        
        return new LinkedList<MarshalledField>(fields);
    }
    
    public Object marshallIn(Object entity, Map<String,String[]> parameterMap, Controller controller) throws MarshallingException {
        Field     field;
        Converter converter;
        Method    setter;
        String[]  newValues;
        
        log.debug("Marshalling in instance of " + entity.getClass().getSimpleName());
        
        boolean error        = false,
                success      = false,
                isEntity     = false,
                isCollection = false;
        
        Class collectionType;
        Class fieldType;
        
        for (Map.Entry<Field,Converter> ent : instructions.entrySet()) {
            
            // reset flags
            error = success = isEntity = isCollection = false;
            
            field     = ent.getKey();
            converter = ent.getValue();
            
            if (converter.isReadOnly()) {
                log.debug("Skipping read-only field " + field.getName());
                continue;
            }
            
            newValues = parameterMap.get(field.getName());
            
            if (newValues == null || newValues.length == 0) {
                log.debug("Skipping field " + field.getName() + ", no new value set.");
                continue;
            }
            
            isEntity     = converter.isEntity();
            isCollection = converter.isCollection();
            
            fieldType      = field.getType();
            collectionType = isCollection ? converter.getType() : null;
            
            log.debug("Marshalling in field " + field.getName());
            
            // try to get the original value
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (field.isAccessible()) {
                    log.debug(field.getName() + " is accessible");
                    if (!isEntity && !isCollection) {
                        
                        log.debug("!isEntity && !isCollection");
                        
                        // if it's an array, it needs special treatment
                        if (fieldType.isArray()) {
                            log.debug(field.getName() + " is an Array");
                            
                            Class arrayType = fieldType.getComponentType();
                            
                            // If we can, just convert with a cast()
                            if (arrayType.isAssignableFrom(String.class)) {
                                log.debug(arrayType.getName() + " is assignable from String.class");
                                
                                Object[] newArray = new Object[newValues.length];
                                for (int i = 0; i < newValues.length; i++) {
                                    newArray[i] = arrayType.cast(newValues[i]);
                                }
                                field.set(entity,newArray);
                                
                            } else {
                                
                                if (isInteger(fieldType)) {
                                    
                                    Integer[] newArray = new Integer[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Integer.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else if (isFloat(fieldType)) {
                                    
                                    Float[] newArray = new Float[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Float.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else if (isDouble(fieldType)) {
                                    
                                    Double[] newArray = new Double[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Double.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else if (isShort(fieldType)) {
                                    
                                    Short[] newArray = new Short[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Short.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else if (isChar(fieldType)) {
                                    
                                    field.set(entity,ServletUtils.join(newValues,"").toCharArray());
                                    
                                } else if (isLong(fieldType)) {
                                    
                                    Long[] newArray = new Long[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Long.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else if (isBoolean(fieldType)) {
                                    
                                    Boolean[] newArray = new Boolean[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Boolean.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else if (isByte(fieldType)) {
                                    
                                    Byte[] newArray = new Byte[newValues.length];
                                    for (int i = 0; i < newValues.length; i++) {
                                        newArray[i] = Byte.valueOf(newValues[i]);
                                    }
                                    field.set(entity,newArray);
                                    
                                } else {
                                    throw new MarshallingException("Don't know how to marshall an array of a non-primitive, and non-assignable type! field = " + field.getName());
                                }
                            }
                            
                        } else {
                            
                            // Check out if it's assignable via a straight cast,
                            // that could save time
                            if (fieldType.isAssignableFrom(String.class)) {
                                log.debug(fieldType.getName() + " is assignable from String.class");
                                // this might throw an exception, but we're going
                                // to ignore it because there are other ways of
                                // setting the value if this doesn't work.
                                try {
                                    field.set(entity,fieldType.cast(newValues[0]));
                                    log.debug("Assigned via cast");
                                } catch (Exception e) {
                                    log.debug("Error setting field by cast",e);
                                }
                                success = true;
                            }
                            
                            // if it wasn't assignable via a straight cast, try
                            // working around it.
                            if (!success) {
                                if (isInteger(fieldType) && !newValues[0].equals("")) {
                                    field.setInt(entity,Integer.valueOf(newValues[0]));
                                } else if (isFloat(fieldType) && !newValues[0].equals("")) {
                                    field.setFloat(entity,Float.valueOf(newValues[0]));
                                } else if (isDouble(fieldType) && !newValues[0].equals("")) {
                                    field.setDouble(entity,Double.valueOf(newValues[0]));
                                } else if (isShort(fieldType) && !newValues[0].equals("")) {
                                    field.setShort(entity,Short.valueOf(newValues[0]));
                                } else if (isChar(fieldType)) {
                                    field.setChar(entity,newValues[0].charAt(0));
                                } else if (isLong(fieldType) && !newValues[0].equals("")) {
                                    field.setLong(entity,Long.valueOf(newValues[0]));
                                } else if (isBoolean(fieldType) && !newValues[0].equals("")) {
                                    field.setBoolean(entity,Boolean.valueOf(newValues[0]));
                                } else if (isByte(fieldType) && !newValues[0].equals("")) {
                                    field.setByte(entity,Byte.valueOf(newValues[0]));
                                } else if (isDate(fieldType)) {
                                    if (newValues[0].equals("")) {
                                        field.set(entity,null);
                                    } else {
                                        try {
                                            field.set(entity,asDate(newValues[0]));
                                        } catch (ParseException pe) {
                                            log.warn("Error parsing date: " + newValues[0],pe);
                                        }
                                    }
                                } else if (!newValues[0].equals("")) {
                                    log.debug("Not sure how to set " + field.getName() + " of type " + fieldType.getName() + ", attemping cast.");
                                    field.set(entity,fieldType.cast(newValues[0]));
                                } else if (newValues[0].equals("")) {
                                    log.debug("Skipping field " + field.getName() + ", empty string value passed in.");
                                }
                            }
                        }
                        
                    } else if (isEntity && !isCollection) {
                        
                        log.debug("isEntity && !isCollection");
                        
                        ReflectionMarshaller innerMarshaller = ReflectionMarshaller.getForClass(fieldType);
                        field.set(entity,controller.proxy(fieldType, innerMarshaller.asIdType(newValues[0])));
                        
                    } else if (!isEntity && isCollection) {
                        
                        log.debug("!isEntity && isCollection");
                        
                        throw new MarshallingException("Error, collections of non-entities are not yet supported.");
                        
                    } else if (isEntity && isCollection) {
                        
                        log.debug("isEntity && isCollection");
                        
                        // for now, this is going to expect the parameter to be a
                        // comma-delimited string of entity ids
                        String[] idsString = newValues[0].toString().split(",");
                        Collection collection = (Collection)field.get(entity);
                        
                        log.debug("newValues.length = " + newValues.length);
                        log.debug("newValues[0] = " + newValues[0]);
                        log.debug("idsString.length = " + idsString.length);
                        
                        if (collection == null)
                            collection = new LinkedList();
                        
                        collection.clear();
                        
                        if (idsString.length > 0) {

                            ReflectionMarshaller collectionMarshaller = ReflectionMarshaller.getForClass(collectionType);
                            
                            log.debug("CollectionType = " + collectionType.getName());

                            for (String idString : idsString) {
                                if (idString.equals("")) {
                                    log.debug("Skipping empty idString");
                                    continue;
                                }
                                collection.add(controller.proxy(collectionType,collectionMarshaller.asIdType(idString)));
                            }
                            
                        }
                        
                        field.set(entity,collection);
                    }
                } else {
                    error = true;
                }
            } catch (IllegalAccessException iae) {
                log.error("Unable to set " + field.getName() + " directly.",iae);
                error = true;
            } catch (ClassCastException cce) {
                log.error("Error setting " + field.getName() + ".",cce);
                error = true;
            }
            
            // if we hit an error getting it directly, try via the getter
            if (error) {
                error = false;
                try {
                    setter = converter.getSetter();
                    if (setter != null) {
                        if (!setter.isAccessible()) {
                            setter.setAccessible(true);
                        }
                        if (setter.isAccessible()) {
                            if (!isEntity && !isCollection) {
                                
                                // if it's an array, it needs special treatment
                                if (fieldType.isArray()) {
                                    log.debug(field.getName() + " is an Array");
                                    
                                    Class arrayType = fieldType.getComponentType();
                                    
                                    // If we can, just convert with a cast()
                                    if (arrayType.isAssignableFrom(String.class)) {
                                        log.debug(arrayType.getName() + " is assignable from String.class");
                                        
                                        Object[] newArray = new Object[newValues.length];
                                        for (int i = 0; i < newValues.length; i++) {
                                            newArray[i] = arrayType.cast(newValues[i]);
                                        }
                                        setter.invoke(entity,newArray);
                                        
                                    } else {
                                        
                                        if (isInteger(fieldType)) {
                                            
                                            Integer[] newArray = new Integer[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Integer.valueOf(newValues[i]);
                                            }
                                            setter.invoke(entity,(Object[])newArray);
                                            
                                        } else if (isFloat(fieldType)) {
                                            
                                            Float[] newArray = new Float[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Float.valueOf(newValues[i]);
                                            }
                                            setter.invoke(entity,(Object[])newArray);
                                            
                                        } else if (isDouble(fieldType)) {
                                            
                                            Double[] newArray = new Double[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Double.valueOf(newValues[i]);
                                            }
                                            setter.invoke(entity,(Object[])newArray);
                                            
                                        } else if (isShort(fieldType)) {
                                            
                                            Short[] newArray = new Short[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Short.valueOf(newValues[i]);
                                            }
                                            setter.invoke(entity,(Object[])newArray);
                                            
                                        } else if (isChar(fieldType)) {
                                            
                                            setter.invoke(entity,ServletUtils.join(newValues,"").toCharArray());
                                            
                                        } else if (isLong(fieldType)) {
                                            
                                            Long[] newArray = new Long[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Long.valueOf(newValues[i]);
                                            }
                                            field.set(entity,(Object[])newArray);
                                            
                                        } else if (isBoolean(fieldType)) {
                                            
                                            Boolean[] newArray = new Boolean[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Boolean.valueOf(newValues[i]);
                                            }
                                            setter.invoke(entity,(Object[])newArray);
                                            
                                        } else if (isByte(fieldType)) {
                                            
                                            Byte[] newArray = new Byte[newValues.length];
                                            for (int i = 0; i < newValues.length; i++) {
                                                newArray[i] = Byte.valueOf(newValues[i]);
                                            }
                                            setter.invoke(entity,(Object[])newArray);
                                            
                                        } else {
                                            throw new MarshallingException("Don't know how to marshall an array of a non-primitive, and non-assignable type! field = " + field.getName());
                                        }
                                    }
                                    
                                } else {
                                    // Check out if it's assignable via a straight cast,
                                    // that could save time
                                    if (fieldType.isAssignableFrom(String.class)) {
                                        log.debug(fieldType.getName() + " is assignable from String.class");
                                        // this might throw an exception, but we're going
                                        // to ignore it because there are other ways of
                                        // setting the value if this doesn't work.
                                        try {
                                            setter.invoke(entity,fieldType.cast(newValues[0]));
                                        } catch (Exception e) {
                                            log.debug("Error setting field by cast",e);
                                        }
                                        success = true;
                                    }
                                    
                                    // if it wasn't assignable via a straight cast, try
                                    // working around it.
                                    if (!success) {
                                        if (isInteger(fieldType)) {
                                            setter.invoke(entity,Integer.valueOf(newValues[0]));
                                        } else if (isFloat(fieldType)) {
                                            setter.invoke(entity,Float.valueOf(newValues[0]));
                                        } else if (isDouble(fieldType)) {
                                            setter.invoke(entity,Double.valueOf(newValues[0]));
                                        } else if (isShort(fieldType)) {
                                            setter.invoke(entity,Short.valueOf(newValues[0]));
                                        } else if (isChar(fieldType)) {
                                            setter.invoke(entity,newValues[0].charAt(0));
                                        } else if (isLong(fieldType)) {
                                            setter.invoke(entity,Long.valueOf(newValues[0]));
                                        } else if (isBoolean(fieldType)) {
                                            setter.invoke(entity,Boolean.valueOf(newValues[0]));
                                        } else if (isByte(fieldType)) {
                                            setter.invoke(entity,Byte.valueOf(newValues[0]));
                                        } else if (isDate(fieldType)) {
                                            if (newValues[0].equals("")) {
                                                field.set(entity,null);
                                            } else {
                                                try {
                                                    setter.invoke(entity,asDate(newValues[0]));
                                                } catch (ParseException pe) {
                                                    log.warn("Error parsing date: " + newValues[0],pe);
                                                }
                                            }
                                        } else {
                                            log.debug("Not sure how to set " + field.getName() + " of type " + fieldType.getName() + ", attemping cast.");
                                            setter.invoke(entity,fieldType.cast(newValues[0]));
                                        }
                                    }
                                }
                                
                            } else if (isEntity && !isCollection) {
                                
                                ReflectionMarshaller innerMarshaller = ReflectionMarshaller.getForClass(fieldType);
                                setter.invoke(entity,controller.proxy(fieldType,innerMarshaller.asIdType(newValues[0])));
                                
                            } else if (!isEntity && isCollection) {
                                
                                throw new MarshallingException("Error, collections of non-entities are not yet supported.");
                                
                            } else if (isEntity && isCollection) {
                                // for now, this is going to expect the parameter to be a
                                // comma-delimited string of entity ids
                                String[] idsString = newValues[0].toString().split(",");
                                Collection collection = (Collection)field.get(entity);

                                if (collection == null)
                                    collection = new LinkedList();

                                if (idsString.length == 0 && collection.isEmpty())
                                    continue;

                                collection.clear();

                                if (idsString.length > 0) {

                                    ReflectionMarshaller collectionMarshaller = ReflectionMarshaller.getForClass(collectionType);

                                    for (String idString : idsString) {
                                        if (idString.equals("")) {
                                            log.debug("Skipping empty idString");
                                            continue;
                                        }
                                        collection.add(controller.proxy(collectionType,collectionMarshaller.asIdType(idString)));
                                    }
                                }
                                
                                setter.invoke(entity,collection);
                            }
                        } else {
                            error = true;
                        }
                    } else {
                        error = true;
                    }
                } catch (IllegalAccessException iae) {
                    log.error("Error accessing setter", iae);
                    error = true;
                } catch (InvocationTargetException ite) {
                    log.error("Error invoking setter", ite);
                    error = true;
                }
            }
            
            if (error) {
                throw new MarshallingException("Unable to marshall in field " + field.getName() + ".");
            }
        }
        return entity;
    }
    
    private static String capitalizeIndex(String string, int index) {
        if (index >= string.length())
            return string;
        
        return string.substring(0,index) + Character.toUpperCase(string.charAt(index)) + string.substring(index+1);
    }
    
    private static Object convertNull(Object obj) {
        return obj == null ? "" : obj;
    }
    
    public String getIdName() {
        return this.entityIdField.getName();
    }
    
    public Class getIdType() {
        return this.entityIdField.getType();
    }
    
    public Object asIdType(String stringId) {
        Class type = getIdType();
        if (type.equals(String.class))
            return stringId;
        if (isInteger(type))
            return Integer.parseInt(stringId);
        if (isLong(type))
            return Long.parseLong(stringId);
        if (isShort(type))
            return Short.parseShort(stringId);
        
        log.debug("Could not convert string to entityIdType (" + type.getName() + ") : " + stringId);
        return stringId;
    }
    
    public boolean isInteger(Class klass) {
        return klass.equals(Integer.class) || klass.equals(int.class);
    }
    
    public boolean isLong(Class klass) {
        return klass.equals(Long.class) || klass.equals(long.class);
    }
    
    public boolean isDouble(Class klass) {
        return klass.equals(Double.class) || klass.equals(double.class);
    }
    
    public boolean isFloat(Class klass) {
        return klass.equals(Float.class) || klass.equals(float.class);
    }
    
    public boolean isBoolean(Class klass) {
        return klass.equals(Boolean.class) || klass.equals(boolean.class);
    }
    
    public boolean isShort(Class klass) {
        return klass.equals(Short.class) || klass.equals(short.class);
    }
    
    public boolean isByte(Class klass) {
        return klass.equals(Byte.class) || klass.equals(byte.class);
    }
    
    public boolean isChar(Class klass) {
        return klass.equals(Character.class) || klass.equals(char.class);
    }
    
    public boolean isDate(Class klass) {
        Class superClass = klass;
        do {
            if (superClass.equals(Date.class))
                return true;
            
            superClass = klass.getSuperclass();
        } while (superClass != null && !superClass.equals(Object.class));
        return false;
    }
    
    public Date asDate(String value) throws ParseException {
        SimpleDateFormat format = null;
        
        for (String pattern : SUPPORTED_DATE_FORMATS) {
            format = new SimpleDateFormat(pattern);
            try {
                return format.parse(value);
            } catch (ParseException pe) { }
        }
        
        throw new ParseException("Unknown date format!", -1);
    }
}