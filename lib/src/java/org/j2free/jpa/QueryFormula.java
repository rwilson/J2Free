/*
 * QueryFormula.java
 *
 *
 *  Author: Ryan Wilson
 */

package org.j2free.jpa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.j2free.util.KeyValuePair;

/**
 *
 * @author ryan
 */
public class QueryFormula {
    
    private String query;
    private HashMap<String, Object> parameters;
    
    public QueryFormula(String query) {
        this.query = query;
        this.parameters = new HashMap<String, Object>();
    }
    
    public QueryFormula(String query, HashMap<String, Object> params) {
        this.query = query;
        this.parameters = params;
    }
    
    public QueryFormula(String query, KeyValuePair<String,Object>... params) {
        this.query = query;
        this.parameters = new HashMap<String, Object>();
        
        for (KeyValuePair<String,Object> pair : params)
            this.parameters.put(pair.key,pair.value);
    }
    
    public String getQuery() { 
        return query; 
    }
    
    public HashMap<String, Object> getParameters() {
        return parameters;
    }

    public KeyValuePair[] getParametersAsPairArray() {
        KeyValuePair[] pairs = new KeyValuePair[parameters.size()];
        
        Iterator<Map.Entry<String,Object>> itr = parameters.entrySet().iterator();
        Map.Entry<String,Object> entry;
        int i = 0;
        while (itr.hasNext()) {
            entry = itr.next();
            pairs[i] = new KeyValuePair<String,Object>(entry.getKey(),entry.getValue());
            i++;
        }
        
        return pairs;
    }
}