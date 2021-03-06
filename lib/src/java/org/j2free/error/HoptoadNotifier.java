/*
 * HoptoadNotifier.java
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
package org.j2free.error;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.http.HttpCallResult;
import org.j2free.http.HttpCallTask;
import org.j2free.http.SimpleHttpService;
import org.j2free.http.HttpCallTask.Method;
import org.j2free.util.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

/**
 * Implements Appender so it can be used directly,
 * or indirectly via logging.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class HoptoadNotifier
{
    private final String NOTIFIER_VERSION = "0.1";

    private final String API_URL = "http://hoptoadapp.com/notifier_api/v2/notices";

    private final Log log = LogFactory.getLog(getClass());

    private final String token;
    private final String version;
    private final boolean validating;

    /**
     *
     * @param token
     * @param version
     */
    public HoptoadNotifier(String token, String version)
    {
        this(token, version, false);
    }

    /**
     *
     * @param token
     * @param version
     * @param validating
     */
    public HoptoadNotifier(String token, String version, boolean validating)
    {
        if (token == null || token.equals(""))
            throw new IllegalArgumentException("Null or blank hoptoad token!");
        if (version == null || version.equals(""))
            throw new IllegalArgumentException("Null or blank hoptoad api version!");

        this.token = token;
        this.version = version;
        this.validating = validating;
    }

    /* --- SET OF notify FUNCTIONS THAT REQUIRE A NON-NULL Throwable --- */

    /**
     * @param thrown A non-null Throwable
     * @return
     * @throws IllegalArgumentException is thrown is null
     */
    public Future<HttpCallResult> notify(Throwable thrown)
    {
        return notify(null, thrown, thrown.getMessage());
    }

    /**
     * @param thrown A non-null Throwable
     * @param message A optional string message; if null, the message from thrown will be used
     * @return
     * @throws IllegalArgumentException is thrown is null
     */
    public Future<HttpCallResult> notify(Throwable thrown, String message)
    {
        return notify(null, thrown, message);
    }

    /**
     * @param request
     * @param thrown
     * @return
     * @throws IllegalArgumentException is thrown is null
     */
    public Future<HttpCallResult> notify(HttpServletRequest request, Throwable thrown)
    {
        return notify(HoptoadContext.parseRequest(request), thrown);
    }

    /**
     *
     * @param context
     * @param thrown
     * @return
     */
    public Future<HttpCallResult> notify(HoptoadContext context, Throwable thrown)
    {
        return notify(context, thrown, null);
    }

    /**
     * @param context
     * @param thrown A non-null Throwable
     * @param message A optional string message; if null, the message from thrown will be used
     * @return
     * @throws IllegalArgumentException is thrown is null
     */
    public Future<HttpCallResult> notify(HoptoadContext context, Throwable thrown, String message)
    {
        if (thrown == null)
            throw new IllegalArgumentException("A non-null Throwable is required when not specifying an explicit StackTraceElement!");
        
        return notify(context, thrown, null, message);
    }

    /* --- SET OF notify FUNCTIONS THAT REQUIRE A NON-NULL StackTraceElement --- */

    /**
     * @param frame A non-null StackTraceElement
     * @return
     * @throws IllegalArgumentException is frame is null
     */
    public Future<HttpCallResult> notify(StackTraceElement frame)
    {
        return notify(frame, null);
    }
    
    /**
     * @param frame A non-null StackTraceElement
     * @param message A optional string message
     * @return
     * @throws IllegalArgumentException is frame is null
     */
    public Future<HttpCallResult> notify(StackTraceElement frame, String message)
    {
        return notify(null, null, frame, message);
    }

    /**
     * @param request
     * @param frame A non-null StackTraceElement
     * @param message A optional string message
     * @return
     * @throws IllegalArgumentException is frame is null
     */
    public Future<HttpCallResult> notify(HttpServletRequest request, StackTraceElement frame, String message)
    {
        return notify(HoptoadContext.parseRequest(request), frame, message);
    }

    /**
     *
     * @param context
     * @param frame
     * @param message
     * @return
     */
    public Future<HttpCallResult> notify(HoptoadContext context, StackTraceElement frame, String message)
    {
        if (frame == null)
            throw new IllegalArgumentException("A non-null StackTraceElement is required when not specifying a non-null Throwable!");

        return notify(context, null, frame, message);
    }

    /* --- PRIVATE IMPL OF notify --- */

    private Future<HttpCallResult> notify(HoptoadContext context, Throwable thrown, StackTraceElement frame, String message)
    {
        if (thrown == null && frame == null)
            throw new IllegalArgumentException("notify requires either a non-null Throwable or non-null StackTraceElement!");

        Document doc = new DocumentImpl();

        // create the doc
        log.trace("Creating Hoptoad Notice");
        Element notice = doc.createElement("notice");
        notice.setAttribute("version", version);

        // Add the <api-key>
        log.trace("adding APIKeyNode");
        notice.appendChild( createAPIKeyNode(doc) );

        // Describe notifier: notifier/[name|version|url]
        log.trace("adding notifier node");
        notice.appendChild( createNotifierNode(doc) );

        // Describe the error: error/[class|message?|backtrace/line+]
        log.trace("adding error node");
        notice.appendChild( createErrorNode(doc, thrown, message, frame) );

        // Describe the request: request/[url|component|action?|params/var?|session/var?|cgi-data/var@key?]?
        if (context != null)
        {
            log.trace("adding request node");
            notice.appendChild( createRequestNode(doc, context) );
        }
        else
            log.trace("request null, skipping node");

        // Describe the server environment: server-environment/[project-root?|environment-name]
        log.trace("adding server-env node");
        notice.appendChild( createServerEnvironmentNode(doc) );

        // wrap it up
        doc.appendChild(notice);

        String xml;
        try
        {
            log.trace("Serializing XML");
            DOMSource domSource = new DOMSource(doc);
            StringWriter xmlWriter = new StringWriter();
            StreamResult result = new StreamResult(xmlWriter);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            xml = xmlWriter.toString();

            if (log.isDebugEnabled())
                log.debug(xml);
        }
        catch (Exception ex)
        {
            log.error("Error serializing XML", ex);
            return null;
        }

        if (!validating || validateXML(xml))
        {
            log.trace("sending to hoptoad");
            return sendToHoptoad(xml);
        }
        else
        {
            log.error("Validation failed!");
            return null;
        }
    }

    private Element createAPIKeyNode(Document doc)
    {
        Element node = doc.createElement("api-key");
        node.appendChild( doc.createTextNode(token) );
        return node;
    }

    private Element createNotifierNode(Document doc)
    {
        Element node = doc.createElement("notifier");

        Element e = doc.createElement("name");
        e.appendChild( doc.createTextNode("J2Free Notifier") );
        node.appendChild(e);

        e = doc.createElement("version");
        e.appendChild( doc.createTextNode(NOTIFIER_VERSION) );
        node.appendChild(e);

        e = doc.createElement("url");
        e.appendChild( doc.createTextNode("http://j2free.org") );
        node.appendChild(e);

        return node;
    }

    /**
     * error/[class|message?|backtrace/line+]
     */
    private Element createErrorNode(Document doc, Throwable thrown, String message, StackTraceElement lastFrame)
    {
        Element node = doc.createElement("error");

        // <class>
        Element e = doc.createElement("class");
        if (thrown != null)
            e.appendChild( doc.createTextNode( thrown.getClass().getSimpleName() ) );
        node.appendChild(e);

        // <message>
        e = doc.createElement("message");
        if (message != null)
            e.appendChild( doc.createTextNode(message) );
        else if (thrown != null)
            e.appendChild(doc.createTextNode(thrown.getMessage()));
        node.appendChild(e);

        // <backtrace>
        node.appendChild( createBacktraceNode(doc, thrown, lastFrame) );

        return node;
    }

    private Element createBacktraceNode(Document doc, Throwable thrown, StackTraceElement lastFrame)
    {
        Element node = doc.createElement("backtrace"),  e;
        if (thrown != null)
        {
            for (StackTraceElement frame : thrown.getStackTrace())
            {
                e = doc.createElement("line");
                e.setAttribute("file", frame.getFileName());
                e.setAttribute("number", String.valueOf(frame.getLineNumber()));
                e.setAttribute("method", frame.getMethodName());
                node.appendChild(e);
            }

            Throwable rootCause = unwindException(thrown);
            if (rootCause != null && rootCause != thrown)
            {
                // DIVIDER --- 
                e = doc.createElement("line");
                e.setAttribute("file", "--------------------");
                e.setAttribute("number", "0");
                e.setAttribute("method", "--------------------");
                node.appendChild(e);

                for (StackTraceElement frame : rootCause.getStackTrace())
                {
                    e = doc.createElement("line");
                    e.setAttribute("file", frame.getFileName());
                    e.setAttribute("number", String.valueOf(frame.getLineNumber()));
                    e.setAttribute("method", frame.getMethodName());
                    node.appendChild(e);
                }
            }
        }
        else if (lastFrame != null)
        {
            e = doc.createElement("line");
            e.setAttribute("file", lastFrame.getFileName());
            e.setAttribute("number", String.valueOf(lastFrame.getLineNumber()));
            e.setAttribute("method", lastFrame.getMethodName());
            node.appendChild(e);
        }
        else
        {
            e = doc.createElement("line");
            e.setAttribute("file", "unknown");
            e.setAttribute("number", "0");
            e.setAttribute("method", "unknown");
            node.appendChild(e);
        }
        return node;
    }

    /**
     * request/[url|component|action?|params/var?|session/var?|cgi-data/var?]?
     */
    @SuppressWarnings("unchecked")
    private Element createRequestNode(Document doc, HoptoadContext context)
    {
        Element node = doc.createElement("request");

        // <url>
        Element e = doc.createElement("url");
        e.appendChild(doc.createTextNode(context.getUrl()));
        node.appendChild(e);

        // <component>
        e = doc.createElement("component");
        e.appendChild(doc.createTextNode(context.getComponent()));
        node.appendChild(e);

        // <action>

        // <params>
        Map params = context.getQueryParams();
        if (params != null && !params.isEmpty())
        {
            e = doc.createElement("params");
            for (Object key : params.keySet())
            {
                if (params.get(key) instanceof String)
                {
                    e.appendChild(
                        createVar(doc, key.toString(), params.get(key).toString())
                    );
                }
                else if (params.get(key) instanceof String[])
                {
                    String[] values = (String[])params.get(key);
                    for (String value : values)
                    {
                        e.appendChild(
                            createVar(doc, key.toString(), value)
                        );
                    }
                }
            }
            node.appendChild(e);
        }

        // <session>
        Map<String, Object> attrs = context.getSessionAttrs();
        if (attrs != null && !attrs.isEmpty())
        {
            e = doc.createElement("session");
            for (String key : attrs.keySet())
            {
                e.appendChild(
                    createVar(doc, key, attrs.get(key).toString())
                );
            }
            node.appendChild(e);
        }

        // <cgi-data>
        e = doc.createElement("cgi-data");
        e.appendChild(
            createVar(doc, "SERVER_NAME", context.getServerName())
        );
        e.appendChild(
            createVar(doc, "REMOTE_ADDR", context.getRemoteAddr())
        );
        e.appendChild(
            createVar(doc, "PATH_INFO", context.getPathInfo())
        );
        e.appendChild(
            createVar(doc, "METHOD", context.getMethod())
        );
        e.appendChild(
            createVar(doc, "USER-AGENT", context.getUserAgent())
        );
        node.appendChild(e);

        return node;
    }

    private Element createVar(Document doc, String key, String value)
    {
        Element var = doc.createElement("var");
        var.setAttribute("key", key);
        var.appendChild( doc.createTextNode(value) );
        return var;
    }

    private Element createServerEnvironmentNode(Document doc)
    {
        Element node = doc.createElement("server-environment"), e;

        e = doc.createElement("environment-name");
        e.appendChild(
            doc.createTextNode(Constants.RUN_MODE.name())
        );
        node.appendChild(e);

        return node;
    }

    private Future<HttpCallResult> sendToHoptoad(String xml)
    {
        HttpCallTask task = new HttpCallTask(Method.POST, API_URL, false);
//        task.addRequestHeader( new Header("Content-type", "text/xml") );
        task.setExplicitPostBody(xml);
        return SimpleHttpService.submit(task);
    }

    /**
     * Looks up and returns the root cause of an exception. If none is found, returns
     * supplied Throwable object unchanged. If root is found, recursively "unwraps" it,
     * and returns the result to the user.
     */
    private Throwable unwindException(Throwable thrown)
    {
        if (thrown instanceof SAXException)
        {
            SAXException saxe = (SAXException) thrown;
            if (saxe.getException() != null)
                return unwindException(saxe.getException());
        }
        else if (thrown instanceof SQLException)
        {
            SQLException sqle = (SQLException) thrown;
            if (sqle.getNextException() != null)
                return unwindException(sqle.getNextException());
        }
        else if (thrown.getCause() != null)
            return unwindException(thrown.getCause());

        return thrown;
    }

    /**
     * Unsure if this function even works...
     */
    private boolean validateXML(String xml)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", "http://hoptoadapp.com/hoptoad_2_0.xsd");

        try
        {
            DocumentBuilder parser = factory.newDocumentBuilder();
            parser.parse(xml);
            return true;
        }
        catch (ParserConfigurationException pce)
        {
            log.error("Vaidation error A", pce);
        }
        catch (SAXException saxe)
        {
            log.error("Vaidation error B", saxe);
        }
        catch (IOException ioe)
        {
            log.error("Vaidation error C", ioe);
        }
        return false;
    }
}
