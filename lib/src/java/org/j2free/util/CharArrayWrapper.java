/**
 * CharArrayWrapper.java
 *
 * Created on October 3, 2007, 3:12 AM
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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/** A response wrapper that takes everything the client
 *  would normally output and saves it in one big
 *  character array.
 */
public class CharArrayWrapper extends HttpServletResponseWrapper {
    
    private CharArrayWriter charWriter;
    /** Initializes wrapper.
     *  <P>
     *  First, this constructor calls the parent
     *  constructor. That call is crucial so that the response
     *  is stored and thus setHeader, setStatus, addCookie,
     *  and so forth work normally.
     *  <P>
     *  Second, this constructor creates a CharArrayWriter
     *  that will be used to accumulate the response.
     */
    public CharArrayWrapper(HttpServletResponse response) {
        super(response);
        charWriter = new CharArrayWriter();
    }

    /** When servlets or JSP pages ask for the Writer,
     *  don't give them the real one. Instead, give them
     *  a version that writes into the character array.
     *  The filter needs to send the contents of the
     *  array to the client (perhaps after modifying it).
     */
    public PrintWriter getWriter() {
        return(new PrintWriter(charWriter));
    }
    
    /** Get a String representation of the entire buffer.
     *  <P>
     *  Be sure <B>not</B> to call this method multiple times
     *  on the same wrapper. The API for CharArrayWriter
     *  does not guarantee that it "remembers" the previous
     *  value, so the call is likely to make a new String
     *  every time.
     */
    public String toString() {
        return(charWriter.toString());
    }
    
    /** Get the underlying character array. */
    public char[] toCharArray() {
        return(charWriter.toCharArray());
    }
}
