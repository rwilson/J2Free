/*
 * HibernateMarshaller.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
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
    protected static ConcurrentHashMap<String,HibernateMarshaller> marshallers = new ConcurrentHashMap();

    public static HibernateMarshaller getForClass(Class klass, ClassMetadata meta) {

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

    public Object asIdType(String stringId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object extractId(Object obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getIdName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Class getIdType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object marshallIn(Object entity, Map<String, String[]> parameterMap, Controller controller)
            throws MarshallingException {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<MarshalledField> marshallOut(Object entity, boolean includeTransient)
            throws IllegalArgumentException {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
