/*
 * BatchRestClient.java
 *
 * Created on April 1, 2008, 6:22 PM
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
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ryan Wilson 
 * @deprecated
 */
public abstract class BatchRestClient extends RestClient {
    
    protected static final int MAX_BATCH_SIZE = 15;
    
    protected boolean batchingEnabled;
    protected List<RestClientMethod> methods;    
    
    public BatchRestClient() throws MalformedURLException {
        this(-1);
    }
    
    public BatchRestClient(int timeout) throws MalformedURLException {
        super(timeout);
        batchingEnabled = false;
        methods         = new LinkedList<RestClientMethod>();
    }
    
    /**
     * Starts a batch of methods.  Any API calls made after invoking 'beginBatch' will be deferred
     * until the next time you call 'executeBatch', at which time they will be processed as a
     * batch query.  All API calls made in the interim will return null as their result.
     */
    public void beginBatch() {
        this.batchingEnabled = true;
        this.methods.clear();
    }
    
    /**
     * Executes a batch of methods.  The API must support batching for this to be
     * feasible.  This class cannot be instantiating without knowing anything
     * about the API, thus this method is abstract and requires a subclass to
     * implement.
     * 
     * @param methods A JSON encoded array of strings. Each element in the array should contain
     *        the full parameters for a method, including method name, sig, etc.
     * @param serial An optional parameter to indicate whether the methods in the method_feed
     *               must be executed in order. The default value is false.
     * @return a result containing the response to each individual query in the batch.
     */
    public abstract <T extends Object> T executeBatch(
            List<RestClientMethod> queries, 
            boolean serial, 
            ResponseHandler<T> handler) 
        throws IOException;
    
    /**
     * Call the specified method, with the given parameters, and return a DOM tree with the results.
     *
     * @param method the fieldName of the method
     * @param parameters a list of arguments to the method
     * @throws Exception with a description of any errors given to us by the server.
     */
    @Override
    protected <T extends Object> T callMethod(
            RestClientMethod method, 
            ResponseHandler<T> handler) 
        throws RestClientException, IOException {
        
        if (!batchingEnabled)
            return super.callMethod(method,handler);
        
        // Add the required parameters
        for(Map.Entry<String,String> param : getRequiredParams().entrySet())
            method.setParameter(param.getKey(),param.getValue());
        
        methods.add(method);
        return null;
    }
}
