/**
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;

/**
 * @author Ryan Wilson
 */
@NotThreadSafe
public class BatchInsert {

    private String table;
    private String[] fields;
    private Class[] types;
    
    private List<Object[]> valuesList;
    
    private String onDuplicateKeyUpdate;

    private boolean ignore;

    /**
     * This version will not attempt to wrap values based on
     * type, but rather call .toString() on them all.
     *
     * @param table The table
     * @param fields an array of the fields
     */
    public BatchInsert(String table, String... fields) {
        this(table, fields, null);
    }

    /**
     * This version will attempt to wrap values based on
     * type, rather than just call .toString() on them all.
     *
     * Currently supported types:
     *  - number primitives and their corresponding wrapper classes
     *  - Date
     *  - String
     *
     * @param table The table
     * @param fields an array of the fields
     * @param types an array of the types of the fields
     */
    public BatchInsert(String table, String[] fields, Class[] types) {
        this.table  = table;
        this.fields = fields;
        this.types  = types;

        valuesList = new LinkedList<Object[]>();
        onDuplicateKeyUpdate = null;
        ignore = false;
    }

    public void onDuplicateKeyUpdate(String action) {
        onDuplicateKeyUpdate = action;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public void add(Object... values) {
        if (values.length != fields.length)
            throw new IllegalArgumentException("Invalid value set, length does not match field length!");

        valuesList.add(values);
    }

    public int size() {
        return valuesList.size();
    }

    public void clear() {
        valuesList.clear();
    }

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();

        // add the table
        if (ignore) {
            query.append(String.format("INSERT IGNORE INTO `%s`", table));
        } else {
            query.append(String.format("INSERT INTO `%s`", table));
        }

        boolean outerFirst = true, innerFirst = true;

        // add the fields list
        if (fields != null && fields.length > 0) {
            query.append("( ");
            for (String field : fields) {
                if (!outerFirst) query.append(",");
                query.append(tick(field));
                outerFirst = false;
            }
            query.append(") ");
        }

        query.append(" VALUES ");

        outerFirst = innerFirst = true;
        Iterator<Object[]> values = valuesList.iterator();
        while (values.hasNext()) {

            if (!outerFirst) query.append(", ");

            query.append(" (");

            innerFirst = true;
            int index = 0;
            for (Object value : values.next()) {
                if (!innerFirst) query.append(",");
                query.append(types == null ? value.toString() : wrapType(value, types[index]));
                innerFirst = false;
                index++;
            }

            query.append(") ");

            outerFirst = false;
        }

        if (onDuplicateKeyUpdate != null)
            query.append(" ON DUPLICATE KEY UPDATE " + onDuplicateKeyUpdate);

        return query.toString();
    }

    private String wrapType(Object o, Class type) {

        if (o == null)
            return "NULL";
        else if (type == null || type.isPrimitive() || isNumberWrapperClass(type)) {
            return o.toString();
        } else if (type.equals(Date.class)) {
            return String.format(
                        "'%s'",
                        (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format((Date)o)
                    );
        } else {
            return String.format("'%s'", ServletUtils.escapeSingleQuotes(o.toString()));
        }
    }

    private boolean isNumberWrapperClass(Class type) {
        return type.equals(Integer.class) ||
               type.equals(Short.class) ||
               type.equals(Float.class) ||
               type.equals(Double.class);
    }

    private String tick(String s) {
        return String.format("`%s`", s);
    }

}
