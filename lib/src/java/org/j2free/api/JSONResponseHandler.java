/*
 * JSONResponseHandler.java
 *
 * Created on April 1, 2008, 5:33 PM
 *
 * Inspired by, and in very limited part adapted from,
 * FacebookJsonRestClient, Copyright (c) 2007 Facebook, Inc
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
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Ryan Wilson (http://blog.augmentedfragments.com)
 */
public class JSONResponseHandler extends ResponseHandler<Object> {
    
    public JSONResponseHandler() {
        super();
    }
    
    public JSONResponseHandler(boolean debug) {
        super(debug);
    }
        
    /**
     * Parses the result of an API call from JSON into Java Objects.
     * 
     * @param in an InputStream with the results of a request to the API servers
     * @param method the method
     * @return a Java Object
     * @see JSONObject
     * @throws IOException if <code>data</code> is not readable
     * @throws RestClientException if unable to parse <code>data</code> 
     */
    protected Object parseCallResult(InputStream in) throws RestClientException, IOException {

        Scanner scanner     = new Scanner(new InputStreamReader(in, "UTF-8"));
        StringBuffer buffer = new StringBuffer();
        
        while (scanner.hasNextLine())
            buffer.append(scanner.nextLine());
        
        String data = new String(buffer);

        log.debug(data);
        
        if (data.matches("[\\{\\[].*[\\}\\]]")) {
            try {
                if (data.matches("\\{.*\\}"))
                    return new JSONObject(data);
                else
                    return new JSONArray(data);
                
            } catch (Exception e) {
                throw new RestClientException(RestClientException.PARSE_ERROR,"Unable to parse JSON response",data);
            }
        } else {
            throw new RestClientException(RestClientException.NOT_JSON,"Response is not JSON",data);
        }
    }

}