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
import org.j2free.util.Pair;

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
    
    public QueryFormula(String query, Pair<String,Object>... params) {
        this.query = query;
        this.parameters = new HashMap<String, Object>();
        
        for (Pair<String,Object> pair : params)
            this.parameters.put(pair.getFirst(),pair.getSecond());
    }
    
    public String getQuery() { 
        return query; 
    }
    
    public HashMap<String, Object> getParameters() {
        return parameters;
    }

    public Pair[] getParametersAsPairArray() {
        Pair[] pairs = new Pair[parameters.size()];
        
        Iterator<Map.Entry<String,Object>> itr = parameters.entrySet().iterator();
        Map.Entry<String,Object> entry;
        int i = 0;
        while (itr.hasNext()) {
            entry = itr.next();
            pairs[i] = new Pair<String,Object>(entry.getKey(),entry.getValue());
            i++;
        }
        
        return pairs;
    }
}