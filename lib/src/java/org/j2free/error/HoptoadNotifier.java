/*
 * HoptoadNotifier.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.error;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
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
import org.j2free.http.HttpCallTask.Method;
import org.j2free.http.SimpleHttpService;
import org.j2free.util.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class HoptoadNotifier
{
    private static final String NOTIFIER_VERSION = "0.1";

    private static final String API_URL = "http://hoptoadapp.com/notifier_api/v2/notices";

    private final Log log = LogFactory.getLog(getClass());
    
    private final String token;
    private final String version;
    private final boolean validating;

    public HoptoadNotifier(String token, String version)
    {
        this(token, version, false);
    }

    public HoptoadNotifier(String token, String version, boolean validating)
    {
        this.token = token;
        this.version = version;
        this.validating = validating;
    }

    public Future<HttpCallResult> notify(Throwable t)
    {
        return notify(t, null);
    }

    public Future<HttpCallResult> notify(Throwable t, String message)
    {
        return notify(null, t, message);
    }

    public Future<HttpCallResult> notify(HttpServletRequest request, Throwable t)
    {
        return notify(request, t, null);
    }

    /**
     * 
     * @param request
     * @param t
     * @param message A message; if null and the specified Throwable is not, then the
     *                message included with the throwable will be used.
     */
    public Future<HttpCallResult> notify(HttpServletRequest request, Throwable t, String message)
    {
        Element e = null;
        Node    n = null;
        Document d = new DocumentImpl();

        // create the doc
        Element notice = d.createElement("notice");
        notice.setAttribute("version", version);

        // Add the <api-key>
        notice.appendChild( createAPIKeyNode(d) );

        // Describe notifier: notifier/[name|version|url]
        notice.appendChild( createNotifierNode(d) );

        // Describe the error: error/[class|message?|backtrace/line+]
        notice.appendChild( createErrorNode(d, t, message) );

        // Describe the request: request/[url|component|action?|params/var?|session/var?|cgi-data/var@key?]?
        if (request != null)
            notice.appendChild( createRequestNode(d, request) );

        // Describe the server environment: server-environment/[project-root?|environment-name]
        notice.appendChild( createServerEnvironmentNode(d, request) );

        // wrap it up
        d.appendChild(notice);

        String xml;
        try
        {
            DOMSource domSource = new DOMSource(d);
            StringWriter xmlWriter = new StringWriter();
            StreamResult result = new StreamResult(xmlWriter);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            xml = xmlWriter.toString();
        }
        catch (Exception ex)
        {
            log.error("Error serializing XML", ex);
            return null;
        }

        if (!validating || validateXML(xml))
            return sendToHoptoad(xml);
        else
        {
            System.out.println("validation failed.");
            log.error("Validation failed!");
            return null;
        }
    }

    private Element createAPIKeyNode(Document d)
    {
        Element node = d.createElement("api-key");
        node.appendChild( d.createTextNode(token) );
        return node;
    }

    private Element createNotifierNode(Document d)
    {
        Element node = d.createElement("notifier");

        Element e = d.createElement("name");
        e.appendChild( d.createTextNode("J2Free Notifier") );
        node.appendChild(e);

        e = d.createElement("version");
        e.appendChild( d.createTextNode(NOTIFIER_VERSION) );
        node.appendChild(e);

        e = d.createElement("url");
        e.appendChild( d.createTextNode("http://j2free.org") );
        node.appendChild(e);

        return node;
    }

    /**
     * error/[class|message?|backtrace/line+]
     */
    private Element createErrorNode(Document d, Throwable t, String message)
    {
        Element node = d.createElement("error");

        // <class>
        Element e = d.createElement("class");
        if (t != null)
            e.appendChild( d.createTextNode( t.getClass().getSimpleName() ) );
        node.appendChild(e);

        // <message>
        e = d.createElement("message");
        if (message != null)
            e.appendChild( d.createTextNode(message) );
        else if (t != null)
            e.appendChild(d.createTextNode(t.getMessage()));
        node.appendChild(e);

        // <backtrace>
        node.appendChild( createBacktraceNode(d, t) );

        return node;
    }

    private Element createBacktraceNode(Document d, Throwable t)
    {
        Element node = d.createElement("backtrace");
        if (t != null)
        {
            Element e;
            for (StackTraceElement frame : t.getStackTrace())
            {
                e = d.createElement("line");
                e.setAttribute("file", frame.getFileName());
                e.setAttribute("number", String.valueOf(frame.getLineNumber()));
                e.setAttribute("method", frame.getMethodName());
                node.appendChild(e);
            }
        }
        return node;
    }

    /**
     * request/[url|component|action?|params/var?|session/var?|cgi-data/var?]?
     */
    private Element createRequestNode(Document d, HttpServletRequest request)
    {
        Element node = d.createElement("request");

        // <url>
        Element e = d.createElement("url");
        e.appendChild(d.createTextNode(request.getRequestURL().toString()));
        node.appendChild(e);

        // <component>
        e = d.createElement("component");
        e.appendChild(d.createTextNode(request.getRequestURI()));
        node.appendChild(e);

        // <action>

        // <params>
        Map params = request.getParameterMap();
        if (params != null && !params.isEmpty())
        {
            e = d.createElement("params");
            for (Map.Entry param : (Set<Map.Entry>)params.entrySet())
            {
                e.appendChild(
                    createVar(d, (String)param.getKey(), param.getValue().toString())
                );
            }
            node.appendChild(e);
        }

        // <session>
        HttpSession session = request.getSession();
        if (session != null)
        {
            e = d.createElement("session");
            Enumeration attrNames = session.getAttributeNames();
            String name;
            while (attrNames.hasMoreElements())
            {
                name = (String)attrNames.nextElement();
                e.appendChild(
                    createVar(d, name, session.getAttribute(name).toString())
                );
            }
            node.appendChild(e);
        }

        // <cgi-data>
        e = d.createElement("cgi-data");
        e.appendChild( 
            createVar(d, "SERVER_NAME", request.getServerName() == null ? "null" : request.getServerName())
        );
        e.appendChild( 
            createVar(d, "REMOTE_ADDR", request.getRemoteAddr() == null ? "null" : request.getRemoteAddr())
        );
        e.appendChild( 
            createVar(d, "PATH_INFO", request.getPathInfo() == null ? "null" : request.getPathInfo())
        );
        e.appendChild( 
            createVar(d, "METHOD", request.getMethod() == null ? "null" : request.getMethod())
        );
        e.appendChild(
            createVar(d, "USER-AGENT", request.getHeader("User-Agent") == null ? "null" : request.getHeader("User-Agent"))
        );
        node.appendChild(e);

        return node;
    }

    private Element createVar(Document d, String key, String value)
    {
        Element var = d.createElement("var");
        var.setAttribute("key", key);
        var.appendChild( d.createTextNode(value) );
        return var;
    }

    private Element createServerEnvironmentNode(Document d, HttpServletRequest request)
    {
        Element node = d.createElement("server-environment"), e;

//        e = d.createElement("project-root");
//        e.appendChild(
//            d.createTextNode(request.getContextPath())
//        );
//        node.appendChild(e);

        e = d.createElement("environment-name");
        e.appendChild( 
            d.createTextNode(Constants.RUN_MODE.name())
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
    private Throwable unwindException(Throwable t)
    {
        if (t instanceof SAXException)
        {
            SAXException saxe = (SAXException) t;
            if (saxe.getException() != null)
                return unwindException(saxe.getException());
        }
        else if (t instanceof SQLException)
        {
            SQLException sqle = (SQLException) t;
            if (sqle.getNextException() != null)
                return unwindException(sqle.getNextException());
        }
        else if (t.getCause() != null)
            return unwindException(t.getCause());

        return t;
    }

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
            log.error("Parse error A", pce);
        }
        catch (SAXException saxe)
        {
            log.error("Parse error B", saxe);
        }
        catch (IOException ioe)
        {
            log.error("Parse error C", ioe);
        }
        return false;
    }
}
