/*
 * QueryFormula.java
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
package org.j2free.jpa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.j2free.util.KeyValuePair;

/**
 * @author Ryan Wilson
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