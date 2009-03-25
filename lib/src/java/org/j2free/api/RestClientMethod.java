/*
 * RestClientMethod.java
 *
 * Created on April 1, 2008, 5:48 PM
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

package org.j2free.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Ryan Wilson (http://blog.augmentedfragments.com)
 */
public abstract class RestClientMethod {

    protected String name;
    
    protected LinkedHashMap<String,String> params;
    
    public RestClientMethod(String name) {
        this.name   = name;
        this.params = new LinkedHashMap<String,String>();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public abstract String formUrl(String serverBase);

    public String getQueryString() {
        StringBuilder buffer = new StringBuilder();
        
        boolean first = true;
        for (Map.Entry<String,String> param : params.entrySet()) {
            if (first)
                first = false;
            else
                buffer.append("&");
            
            buffer.append(param.getKey() + "=" + param.getValue());
        }
        
        return buffer.toString();
    }

    public RestClientMethod setParameter(String param, String value) {
        this.params.put(param,value);
        return setParameter(param,value,true);
    }
    
    public RestClientMethod setParameter(String param, Object val) {
        String value = val == null ? "" : val.toString();
        return setParameter(param,value,true);
    }
    
    public RestClientMethod setParameter(String param, String value, boolean encode) {
        this.params.put(param,encode(value));
        return this;
    }
    
    protected String encode(String target) {
        if (target == null) {
            return "";
        }
        String result = target.toString();
        try {
            result = URLEncoder.encode(result, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // Can't encode, screw it...
        }
        return result;
    }
    
}