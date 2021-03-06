/**
 * RequestExaminerImpl.java
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
package org.j2free.invoker;

import javax.servlet.http.HttpServletRequest;

/**
 * Default implementation of determining if a request is secure.
 *
 * @author Ryan Wilson
 */
class RequestExaminerImpl implements RequestExaminer
{
    public boolean isSSL(HttpServletRequest req)
    {
        return req.isSecure();
    }
}
