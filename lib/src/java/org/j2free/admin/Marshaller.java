/**
 * Marshaller.java
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
package org.j2free.admin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.j2free.jpa.Controller;

/**
 * @author Ryan Wilson
 */
public interface Marshaller {


    /**
     * 
     */
    static class Converter
    {

        private boolean readOnly;
        private boolean collection;
        private boolean entity;

        private Method getter;
        private Method setter;

        private Class type;

        /**
         * 
         * @param getter
         * @param setter
         */
        protected Converter(Method getter, Method setter)
        {
            this.getter      = getter;
            this.setter      = setter;
            this.readOnly    = false;
            this.collection  = false;
            this.entity      = false;
            this.type        = null;
        }

        /**
         * 
         * @return
         */
        protected boolean isReadOnly()
        {
            return readOnly;
        }

        /**
         * 
         * @param readOnly
         */
        protected void setReadOnly(boolean readOnly)
        {
            this.readOnly = readOnly;
        }

        /**
         * 
         * @return
         */
        protected Method getGetter()
        {
            return getter;
        }

        /**
         * 
         * @return
         */
        protected Method getSetter()
        {
            return setter;
        }

        /**
         * 
         * @return
         */
        protected boolean isEntity()
        {
            return entity;
        }

        /**
         * 
         * @param type
         */
        protected void setEntity(Class type)
        {
            this.entity = true;
            this.type   = type;
        }

        /**
         * 
         * @return
         */
        public boolean isCollection()
        {
            return collection;
        }

        /**
         * 
         * @param type
         */
        public void setCollection(Class type)
        {
            this.collection = true;
            this.type       = type;
        }

        /**
         * 
         * @return
         */
        public Class getType()
        {
            return type;
        }
    }

    /**
     * 
     */
    static class MarshalledField implements Comparable<MarshalledField>
    {

        private String  name;
        private Object  value;

        private boolean readOnly;
        private boolean entity;
        private boolean collection;

        private Class type;

        /**
         * 
         * @param name
         */
        protected MarshalledField(String name)
        {
            this(name, "", null, false, false, false);
        }

        /**
         * 
         * @param name
         * @param value
         * @param converter
         */
        protected MarshalledField(String name, Object value, Converter converter)
        {
            this(name, value, converter.getType(), converter.isEntity(), converter.isReadOnly(), converter.isCollection());
        }

        /**
         * 
         * @param name
         * @param value
         * @param collectionType
         * @param entity
         * @param readOnly
         * @param collection
         */
        protected MarshalledField(String name, Object value, Class collectionType, boolean entity, boolean readOnly, boolean collection)
        {
            this.name        = name;
            this.value       = value;
            this.entity      = entity;
            this.readOnly    = readOnly;
            this.collection  = collection;

            this.type = collectionType;
        }

        /**
         * 
         * @return
         */
        public boolean isReadOnly()
        {
            return readOnly;
        }

        /**
         * 
         * @return
         */
        public String getName()
        {
            return name;
        }

        /**
         * 
         * @return
         */
        public Object getValue()
        {
            return value;
        }

        /**
         * 
         * @return
         */
        public boolean isEntity()
        {
            return entity;
        }

        /**
         * 
         * @return
         */
        public boolean isCollection()
        {
            return collection;
        }

        /**
         * 
         * @return
         */
        public Class getType()
        {
            return type;
        }

        /**
         * 
         * @param other
         * @return
         */
        public int compareTo(MarshalledField other)
        {

            if (other == null)
                return 1;

            if (this.equals(other))
                return 0;

            return this.getName().compareTo(other.getName());
        }
    }

    /**
     *
     */
    static final String[] SUPPORTED_DATE_FORMATS = new String[] {
        "yyyy-MM-dd HH:mm:ss",
        "MM-dd-yyyy HH:mm:ss",
        "yyyy-MM-dd hh:mm:ss aa",
        "MM-dd-yyyy hh:mm:ss aa",
        "yyyy-MM-dd",
        "MM-dd-yyyy"
    };

    /**
     *
     * @param stringId
     * @return
     */
    Object asIdType(String stringId);

    /**
     *
     * @param obj
     * @return
     */
    Object extractId(Object obj);

    /**
     *
     * @return
     */
    String getIdName();

    /**
     *
     * @return
     */
    Class getIdType();

    /**
     *
     * @param entity
     * @param parameterMap
     * @param controller
     * @return
     * @throws MarshallingException
     */
    Object marshallIn(Object entity, Map<String, String[]> parameterMap, Controller controller)
            throws MarshallingException;

    /**
     *
     * @param entity
     * @param includeTransient
     * @return
     * @throws IllegalArgumentException
     */
    List<MarshalledField> marshallOut(Object entity, boolean includeTransient)
            throws IllegalArgumentException;

}
