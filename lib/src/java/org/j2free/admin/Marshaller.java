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


    static class Converter {

        private boolean readOnly;
        private boolean collection;
        private boolean entity;

        private Method getter;
        private Method setter;

        private Class type;

        protected Converter(Method getter, Method setter) {
            this.getter      = getter;
            this.setter      = setter;
            this.readOnly    = false;
            this.collection  = false;
            this.entity      = false;
            this.type        = null;
        }

        protected boolean isReadOnly() {
            return readOnly;
        }

        protected void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        protected Method getGetter() {
            return getter;
        }

        protected Method getSetter() {
            return setter;
        }

        protected boolean isEntity() {
            return entity;
        }

        protected void setEntity(Class type) {
            this.entity = true;
            this.type   = type;
        }

        public boolean isCollection() {
            return collection;
        }

        public void setCollection(Class type) {
            this.collection = true;
            this.type       = type;
        }

        public Class getType() {
            return type;
        }
    }

    static class MarshalledField implements Comparable<MarshalledField> {

        private String  name;
        private Object  value;

        private boolean readOnly;
        private boolean entity;
        private boolean collection;

        private Class type;

        protected MarshalledField(String name) {
            this(name, "", null, false, false, false);
        }

        protected MarshalledField(String name, Object value, Converter converter) {
            this(name, value, converter.getType(), converter.isEntity(), converter.isReadOnly(), converter.isCollection());
        }

        protected MarshalledField(String name, Object value, Class collectionType, boolean entity, boolean readOnly, boolean collection) {
            this.name        = name;
            this.value       = value;
            this.entity      = entity;
            this.readOnly    = readOnly;
            this.collection  = collection;

            this.type = collectionType;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public boolean isEntity() {
            return entity;
        }

        public boolean isCollection() {
            return collection;
        }

        public Class getType() {
            return type;
        }

        public int compareTo(MarshalledField other) {

            if (other == null)
                return 1;

            if (this.equals(other))
                return 0;

            return this.getName().compareTo(other.getName());
        }
    }

    static final String[] SUPPORTED_DATE_FORMATS = new String[] {
        "yyyy-MM-dd HH:mm:ss",
        "MM-dd-yyyy HH:mm:ss",
        "yyyy-MM-dd hh:mm:ss aa",
        "MM-dd-yyyy hh:mm:ss aa",
        "yyyy-MM-dd",
        "MM-dd-yyyy"
    };

    Object asIdType(String stringId);

    Object extractId(Object obj);

    String getIdName();

    Class getIdType();

    Object marshallIn(Object entity, Map<String, String[]> parameterMap, Controller controller)
            throws MarshallingException;

    List<MarshalledField> marshallOut(Object entity, boolean includeTransient)
            throws IllegalArgumentException;

}
