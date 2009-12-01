/*
 * RestClient.java
 *
 * Created on April 1, 2008, 5:32 PM
 *
 * Inspired by, and in limited part adapted from, 
 * ExtensibleClient, Copyright (c) 2007 Facebook, Inc
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

import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ryan Wilson 
 * @deprecated
 */
public abstract class RestClient {
    
    protected static Log log = LogFactory.getLog(RestClient.class);
    
    public static final String RESPONSE_FORMAT_XML  = "xml";
    public static final String RESPONSE_FORMAT_JSON = "json";
    
    protected enum HttpMethod { POST, GET };
    
    protected int     timeout;
    protected boolean debug;
    
    protected RestClientException error;
    
    protected static final String CRLF = "\r\n";
    protected static final String PREF = "--";
    
    protected RestClient() {
        this(-1);
    }
    
    protected RestClient(int timeout) {
        this.timeout = timeout;
        debug = false;
        error = null;
    }
    
    protected abstract Map<String,String> getRequiredParams();
    protected abstract HttpMethod getHttpMethod();
    protected abstract String getServerUrl();
    
    public void debug() {
        this.debug = true;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * @return true if the last call completed without error, otherwise false
     */
    public boolean success() {
        return error == null;
    }
    
    /**
     * @return true if the last call completed with error, otherwise false
     */
    public boolean failed() {
        return error != null;
    }
    
    /**
     * @return the RestClientException if thrown in the last call, otherwise null
     */
    public RestClientException getError() {
        return error;
    }
    
    private void clearError() {
        error = null;
    }
    
    /**
     * Extracts a String from a result consisting entirely of a String.
     * @param val
     * @return the String
     */
    protected String extractString(Object val) {
        if (val == null) {
            return null;
        }
        try {
            return (String) val;
        } catch (ClassCastException cce) {
            log.error(cce);
            return null;
        }
    }
    
    /**
     * Extracts a URL from a result that consists of a URL only.
     * For JSON, that result is simply a String.
     * @param url
     * @return the URL
     */
    protected URL extractURL(Object url) throws MalformedURLException {
        if (url == null) {
            return null;
        }
        if (!(url instanceof String)) {
            return null;
        }
        return (null == url || "".equals(url)) ? null : new URL( (String) url);
    }
    
    /**
     * Extracts an Integer from a result that consists of an Integer only.
     * @param val
     * @return the Integer
     */
    protected int extractInt(Object val) {
        if (val == null) {
            return 0;
        }
        try {
            if (val instanceof String) {
                //shouldn't happen, really
                return Integer.parseInt((String)val);
            }
            if (val instanceof Long) {
                //this one will happen, the parse method parses all numbers as longs
                return ((Long)val).intValue();
            }
            return (Integer) val;
        } catch (ClassCastException cce) {
            log.error(cce);
            return 0;
        }
    }
    
    /**
     * Extracts a Boolean from a result that consists of a Boolean only.
     * @param val
     * @return the Boolean
     */
    protected boolean extractBoolean(Object val) {
        if (val == null) {
            return false;
        }
        try {
            if (val instanceof String) {
                return ((val != null) && ((val.equals("true")) || (val.equals("1"))));
            }
            return ((Long)val == 1l);
        } catch (ClassCastException cce) {
            log.error(cce);
            return false;
        }
    }
    
    /**
     * Extracts a Long from a result that consists of an Long only.
     * @param val
     * @return the Integer
     */
    protected Long extractLong(Object val) {
        if (val == null) {
            return 0l;
        }
        try {
            if (val instanceof String) {
                //shouldn't happen, really
                return Long.parseLong((String)val);
            }
            return (Long) val;
        } catch (ClassCastException cce) {
            log.error(cce);
            return null;
        }
    }
    
    /**
     * Call the specified method, automatically parse the result, suppress exceptions
     * 
     * @param method a RestClientMethod object
     * @param handler a ResponseHandler object to parse the result
     * @return an instance the generic type parameter of the ResponseHandler
     * @throws Exception with a description of any errors given to us by the server.
     */
     protected <T extends Object> T wrapCall(RestClientMethod method, ResponseHandler<T> handler) {
         try {
             return callMethod(method,handler);
         } catch (RestClientException rce) {
             error = rce;
         } catch (Exception e) {
             log.error(e);
         }
         return null;
     }
    
    /**
     * Call the specified method, automatically parse the result
     * 
     * @param method a RestClientMethod object
     * @param handler a ResponseHandler object to parse the result
     * @return an instance the generic type parameter of the ResponseHandler
     * @throws Exception with a description of any errors given to us by the server.
     */
    protected <T extends Object> T callMethod(RestClientMethod method, ResponseHandler<T> handler) 
        throws RestClientException, IOException {
        return handler.parseCallResult(getRawStream(method));
    }
    
    /**
     * Call the specified method and return the result as an unmodified string
     *
     * @param method a RestClientMethod to call
     * @throws Exception with a description of any errors
     */
    protected String getRawString(RestClientMethod method) throws IOException {
        
        Scanner scanner    = new Scanner(getRawStream(method));
        StringBuilder data = new StringBuilder();
        
        while (scanner.hasNextLine())
            data.append(scanner.nextLine());
        
        return data.toString();
    }

    /**
     * Call the specified method, with the given parameters, and return a DOM tree with the results.
     * 
     * @param method the fieldName of the method
     * @throws Exception with a description of any errors given to us by the server.
     */
    protected InputStream getRawStream(RestClientMethod method) throws IOException {
        
        error = null;
        
        // Add the required parameters
        for(Map.Entry<String,String> param : getRequiredParams().entrySet())
            method.setParameter(param.getKey(),param.getValue());
        
        String queryString = method.getQueryString();
        
        HttpURLConnection conn = null;
        
        if (getHttpMethod() == HttpMethod.GET) {
            URL callUrl = new URL(method.formUrl(getServerUrl()) + "?" + queryString);
        
            log.debug(method.getName() + " " + getHttpMethod() + ": " + callUrl);

            conn = (HttpURLConnection)callUrl.openConnection();

            if (timeout > 0)
                conn.setConnectTimeout(timeout);

            try {
                conn.setRequestMethod(getHttpMethod().toString());
            } catch (ProtocolException ex) {
                log.error(ex);
            }
            conn.connect();
            
        } else {
            URL callUrl = new URL(method.formUrl(getServerUrl()));
        
            log.debug(method.getName() + " " + getHttpMethod() + ": " + callUrl + "?" + queryString);
        
            conn = (HttpURLConnection)callUrl.openConnection();

            if (timeout > 0)
                conn.setConnectTimeout(timeout);

            try {
                conn.setRequestMethod(getHttpMethod().toString());
            } catch (ProtocolException ex) {
                log.error(ex);
            }

            conn.setDoOutput(true);
            conn.connect();
            conn.getOutputStream().write(queryString.getBytes());
        }
        
        return conn.getInputStream();
    }
    
}