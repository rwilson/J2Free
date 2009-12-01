/*
 * RestClientException.java
 *
 * Created on April 2, 2008, 9:32 AM
 *
 * Copyright (c) 2008 Publi.us
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * The Software shall be used for Good, not Evil.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Copyright language borrowed from JSON.
 */

package org.j2free.api;

/**
 *
 * @author Ryan Wilson 
 * @deprecated
 */
public class RestClientException extends Exception {
    
    public static final int NOT_JSON    = -2;
    public static final int PARSE_ERROR = -1;
    
    private int code;
    private String data;
    
    public RestClientException(int code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    public RestClientException(int code, String message, String data) {
        super(message);
        this.code = code;
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getData() {
        return data;
    }
}
