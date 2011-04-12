/*
 * HibernateMarshaller.java
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.metadata.ClassMetadata;
import org.j2free.jpa.Controller;

/**
 * A Marshalling class that uses the Hibernate ClassMetadata
 *
 * @author Ryan Wilson
 */
public final class HibernateMarshaller implements Marshaller {

    private final Log log = LogFactory.getLog(HibernateMarshaller.class);
    
    /* Maps Class names to previously generated Marshallers so we don't have to
     * go through the expensive process of creating the same marshaller twice.
     */
    /**
     *
     */
    protected static ConcurrentHashMap<String,HibernateMarshaller> marshallers = new ConcurrentHashMap();

    /**
     * 
     * @param klass
     * @param meta
     * @return
     */
    public static HibernateMarshaller getForClass(Class klass, ClassMetadata meta)
    {

        HibernateMarshaller marshaller = marshallers.get(klass.getName());

        if (marshaller != null)
            return marshaller;

        if (marshaller == null) {
            try {
                marshaller = new HibernateMarshaller(klass, meta);
            } catch (Exception e) {
                System.err.println("Error creating marshaller for " + klass.getName());
                e.printStackTrace(System.err);
                return null;
            }
        }

        marshallers.put(klass.getName(),marshaller);
        return marshaller;
    }

    private Class klass;
    private ClassMetadata meta;

    private HibernateMarshaller(Class klass, ClassMetadata meta) throws Exception {

        this.klass = klass;
        this.meta = meta;

    }

    /**
     * 
     * @param stringId
     * @return
     */
    public Object asIdType(String stringId)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * 
     * @param obj
     * @return
     */
    public Object extractId(Object obj)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * 
     * @return
     */
    public String getIdName()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * 
     * @return
     */
    public Class getIdType()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @param entity
     * @param parameterMap
     * @param controller
     * @return
     * @throws MarshallingException
     */
    public Object marshallIn(Object entity, Map<String, String[]> parameterMap, Controller controller)
            throws MarshallingException {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @param entity
     * @param includeTransient
     * @return
     * @throws IllegalArgumentException
     */
    public List<MarshalledField> marshallOut(Object entity, boolean includeTransient)
            throws IllegalArgumentException {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
