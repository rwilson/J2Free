/*
 * UncaughtServletExceptionHandler.java
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Interface class so a J2Free application can provide
 * a way for InvokerFilter to handle uncaught exceptions
 * that bubble up to it's level.
 *
 * @author Ryan Wilson
 */
public interface UncaughtServletExceptionHandler
{
    /**
     *
     * @param request
     * @param response
     * @param e
     * @throws ServletException
     * @throws IOException
     */
    public void handleException(ServletRequest request, ServletResponse response, Throwable e)
            throws ServletException, IOException;
}
