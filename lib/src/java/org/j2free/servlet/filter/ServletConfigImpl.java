/*
 * ServletConfigImpl.java
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
package org.j2free.servlet.filter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import net.jcip.annotations.Immutable;

import org.j2free.annotations.InitParam;
import org.j2free.util.ServletUtils;

/**
 * Final and package level access because it is not needed
 * outside of the InvokerFilter.  It's only a separate file
 * so that the InvokerFilter cannot bypass the accessor
 * methods and get direct access to the unsynchronized HashMap
 * of initParams.
 *
 * @author Ryan Wilson
 */
@Immutable
final class ServletConfigImpl implements ServletConfig {

    private final String name;

    private final ServletContext context;
    
    // We can get away with an unsynchronized map since it is populated in the
    // constructor and never externally published
    private final HashMap<String, String> initParams;

    public ServletConfigImpl(String name, ServletContext context, InitParam[] initParams) {

        this.name    = name;
        this.context = context;

        this.initParams = new HashMap<String, String>();
        
        for (InitParam param : initParams) {
            this.initParams.put(param.name(), param.value());
        }
    }

    public String getServletName() {
        return name;
    }

    public ServletContext getServletContext() {
        return context;
    }

    public String getInitParameter(String key) {
        return initParams.get(key);
    }

    // UGLY HACK: ServletConfig requires getInitParameterNames return an Enumeration, which
    //            is only implemented by StringTokenizer.  Anticipating that we won't ever
    //            actually call this function, this is probably okay, but it's nasty.
    public Enumeration getInitParameterNames() {
        return new StringTokenizer(ServletUtils.join(initParams.keySet(), ","), ",");
    }
}
