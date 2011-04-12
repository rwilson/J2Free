/*
 * MarshallingException.java
 *
 * Created on June 29, 2008, 3:15 PM
 *
 * Copyright (c) 2008 FooBrew, Inc.
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

package org.j2free.admin;

/**
 * @author Ryan Wilson 
 */
public class MarshallingException extends Exception {

    /**
     * 
     */
    public MarshallingException()
    {
        super();
    }
    
    /**
     * 
     * @param message
     */
    public MarshallingException(String message)
    {
        super(message);
    }
    
    /**
     * 
     * @param cause
     */
    public MarshallingException(Throwable cause)
    {
        super(cause);
    }
    
    /**
     * 
     * @param message
     * @param cause
     */
    public MarshallingException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
}
