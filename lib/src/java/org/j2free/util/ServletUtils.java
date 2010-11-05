/*
 * ServletUtils.java
 *
 * Created on September 14, 2007, 4:06 AM
 *
 */
package org.j2free.util;

import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;

import org.j2free.security.SecurityUtils;

import static org.j2free.util.Constants.*;

/**
 *
 * @author Ryan Wilson
 */
public class ServletUtils {

    public static int getIntParameter(HttpServletRequest req, String name) {
        return getIntParameter(req, name, -1);
    }

    public static int getIntParameter(HttpServletRequest req, String name, int defValue) {
        int i = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                i = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static short getShortParameter(HttpServletRequest req, String name) {
        short s = -1;
        return getShortParameter(req, name, s);
    }

    public static short getShortParameter(HttpServletRequest req, String name, short defValue) {
        short i = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                i = Short.parseShort(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static long getLongParameter(HttpServletRequest req, String name, long defValue) {
        long l = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                l = Long.parseLong(s);
            } catch (Exception e) {
            }
        }
        return l;
    }

    public static float getFloatParameter(HttpServletRequest req, String name, float defValue) {
        float i = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                i = Float.parseFloat(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static double getDoubleParameter(HttpServletRequest req, String name, double defValue) {
        double i = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                i = Double.parseDouble(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static boolean getBooleanParameter(HttpServletRequest req, String name, boolean defValue) {
        boolean b = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                b = Boolean.parseBoolean(s);
            } catch (Exception e0) {
            }
        }
        return b;
    }

    public static java.sql.Date getDateParameter(HttpServletRequest req, String name, java.sql.Date defValue) {
        java.sql.Date d = defValue;
        String s = req.getParameter(name);
        if (s != null) {
            try {
                d = java.sql.Date.valueOf(s);
            } catch (Exception e) {
            }
        }
        return d;
    }

    public static String getStringParameter(HttpServletRequest req, String name) {
        return getStringParameter(req, name, EMPTY);
    }

    public static String getStringParameter(HttpServletRequest req, String name, String defValue) {
        String s = req.getParameter(name);
        if (s == null) {
            s = defValue;
        }
        return s == null ? s : s.trim();
    }

    public static String[] getStringArrayParameter(HttpServletRequest req, String name, String del) {
        return getStringArrayParameter(req, name, del, new String[0]);
    }

    public static String[] getStringArrayParameter(HttpServletRequest req, String name, String del, String[] defValue) {
        String s = req.getParameter(name);

        if (s == null) {
            return defValue;
        }

        return splitAndTrim(s, del);
    }

    /**
     * @return the stack trace of a Throwable object as a String
     * @param t the Throwable item
     */
    public static String throwableToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    /**
     * @param href The href attribute
     * @param text The text
     * @return a HTML a tag in String form
     */
    public static String formHyperlink(String href, String text) {
        return String.format("<a href=\"%s\">%s</a>", href, text);
    }

    /** 
     * @param words to join
     * @param separator, the separator to put between words
     * @return String of words concatonated with the separator
     */
    public static String join(Object[] words, String separator) {

        if (ArrayUtils.isEmpty(words)) return EMPTY;

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                ret.append(separator);
            }
            ret.append(words[i].toString());
        }
        return ret.toString();
    }

    /**
     * @param   ints to join
     * @param   separator, the separator to put between words
     * @return  String of words concatonated with the separator
     */
    public static String join(int[] words, String separator) {

        if (ArrayUtils.isEmpty(words)) return EMPTY;

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                ret.append(separator);
            }
            ret.append(words[i]);
        }
        return ret.toString();
    }

    /**
     * @param   ints to join
     * @param   separator, the separator to put between words
     * @return  String of words concatonated with the separator
     */
    public static String join(float[] words, String separator) {

        if (ArrayUtils.isEmpty(words)) return EMPTY;

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                ret.append(separator);
            }
            ret.append(words[i]);
        }
        return ret.toString();
    }

    /**
     * @param   ints to join
     * @param   separator, the separator to put between words
     * @return  String of words concatonated with the separator
     */
    public static String join(double[] words, String separator) {

        if (ArrayUtils.isEmpty(words)) return EMPTY;

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                ret.append(separator);
            }
            ret.append(words[i]);
        }
        return ret.toString();
    }

    /**
     * @param   joined = string to split
     * @param   separator = delimiter to split by
     * @return  Array of Strings
     */
    public static String[] splitAndTrim(String joined, String separator) {

        if (StringUtils.isEmpty(joined)) return new String[0];

        if (separator == null) {
            return new String[]{joined};
        }

        String[] ret = joined.split(separator);
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ret[i].trim();
        }
        return ret;
    }

    /** 
     * @param   words to join
     * @param   separator, the separator to put between words
     * @return  String of words concatonated with the separator
     */
    public static <T extends Object> String join(Collection<T> words, String separator) {

        if (words == null || words.isEmpty()) {
            return EMPTY;
        }

        StringBuilder ret = new StringBuilder();
        Iterator<T> itr = words.iterator();
        int i = 0;
        while (itr.hasNext()) {
            if (i > 0) {
                ret.append(separator);
            }
            ret.append(itr.next().toString());
            i++;
        }
        return ret.toString();
    }

    /**
     * Returns true if the parameter string prior to the "sig" parameter match the "sig"
     * parameter when combined with the key and hashed.  For post requests, the parameter
     * order is not guaranteed and so is assumed to be sorted alphabetically by key.
     *
     * @param a request object
     * @param a secret key
     */
    public static boolean isAuthenticatedRequest(HttpServletRequest req, String secret)
    {
        String query, sig = getStringParameter(req, "sig"), method = req.getMethod();
        if (method.equalsIgnoreCase("GET"))
        {
            query = req.getQueryString();

            if (StringUtils.isBlank(query) || StringUtils.isBlank(sig))
                return false;

            query = query.replace("&sig=" + sig, EMPTY);
        }
        else if (method.equalsIgnoreCase("POST"))
        {
            TreeMap<String, String[]> params = new TreeMap(req.getParameterMap());
            params.remove("sig"); // remove the signature

            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String[]> entry : params.entrySet())
            {
                if (buf.length() > 0)
                    buf.append("&");
                
                buf.append(entry.getKey());
                buf.append('=');
                buf.append(entry.getValue()[0]);
            }
            query = buf.toString();
        }
        else // We're not supporting auth on non GET or POST requests
            return false;
        
        return signQueryString(query, secret).equals(sig);
    }

    public static String signQueryString(String query, String secret) {
        return SecurityUtils.SHA1(query + secret);
    }

    public static void dispatchRequest(HttpServletRequest request, HttpServletResponse response, String destination)
            throws ServletException, IOException {
        dispatchRequest(request, response, destination, false);
    }

    public static void dispatchCompressed(HttpServletRequest request, HttpServletResponse response, String destination)
            throws ServletException, IOException {
        dispatchRequest(request, response, destination, true, "text/html", true, true);
    }

    public static void dispatchCompressed(HttpServletRequest request, HttpServletResponse response, String destination,
                                          String contentType)
            throws ServletException, IOException {
        dispatchRequest(request, response, destination, true, contentType, true, true);
    }

    public static void dispatchRequest(HttpServletRequest request, HttpServletResponse response, String destination,
                                       boolean wrap)
            throws ServletException, IOException {
        dispatchRequest(request, response, destination, wrap, "text/html", false, false);
    }

    public static void dispatchRequest(HttpServletRequest request, HttpServletResponse response, String destination,
                                       boolean wrap, String contentType)
            throws ServletException, IOException {
        dispatchRequest(request, response, destination, wrap, contentType, false, false);
    }

    public static void dispatchRequest(HttpServletRequest request, HttpServletResponse response, String destination,
                                       boolean wrap, String contentType, boolean compress)
            throws ServletException, IOException {
        dispatchRequest(request, response, destination, wrap, contentType, compress, false);
    }

    public static void dispatchRequest(HttpServletRequest request, HttpServletResponse response, String destination,
                                       boolean wrap, String contentType, boolean compress, boolean flushAndClose)
            throws ServletException, IOException {

        RequestDispatcher rd = request.getRequestDispatcher(destination);

        if (!wrap) {

            rd.forward(request, response);

        } else {
            CharArrayWrapper responseWrapper = new CharArrayWrapper((HttpServletResponse) response);
            rd.forward(request, responseWrapper);

            String responseString = responseWrapper.toString();

            if (compress) {
                responseString = compressHTML(responseString);
            }

            response.setContentLength(responseString.length());
            response.setContentType(contentType);

            PrintWriter out = response.getWriter();
            out.write(responseString);

            if (flushAndClose) {
                out.flush();
                out.close();
            }
        }
    }

    public static void sendPermanentRedirect(HttpServletResponse response, String url) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", url);
        response.setHeader("Connection","close");
    }

    /**
     * Redirects the user to the current url over SSL
     *
     * @param request a HttpServletRequest
     * @param response a HttpServletResponse
     * @param sslPort the port SSL requests should be forwarded to
     */
    public static void redirectOverSSL(HttpServletRequest request, HttpServletResponse response, int sslPort)
        throws ServletException, IOException {
        
        StringBuffer url = request.getRequestURL();

        // Make sure we're on https
        if (url.charAt(4) != 's')
            url.insert(4, 's');

        // If there is a ssl port, make sure we're on it,
        // otherwise assume we're already on the right port
        if (sslPort > 0) {
            int portStart = url.indexOf(":", 8) + 1;
            int portEnd   = url.indexOf("/", 8);

            if (portEnd == -1)                              // If their isn't a trailing slash, then the end is the last char
                portEnd = url.length() - 1;

            if (portStart > 0 && portStart < portEnd) {     // If we detected a : before the trailing slash or end of url, delete the port
                url.delete(portStart, portEnd);
            } else {
                url.insert(portEnd, ':');                   // If the url didn't have a port, add in the :
                portStart = portEnd;
            }

            url.insert(portStart, sslPort);   // Insert the right port where it should be
        }

        LogFactory.getLog(ServletUtils.class).debug("redirectOverSSL sending 301: " + url.toString());
        sendPermanentRedirect(response, url.toString());
    }

    /**
     * Redirects the user to the provided url over SSL
     *
     * @param request a HttpServletRequest
     * @param response a HttpServletResponse
     * @param sslPort the port SSL requests should be forwarded to
     */
    public static void redirectOverSSL(HttpServletRequest request, HttpServletResponse response, String urlStr, int sslPort)
        throws ServletException, IOException {

        StringBuffer url = new StringBuffer(urlStr);

        // Make sure we're on https
        if (url.charAt(4) != 's')
            url.insert(4, 's');

        // If there is a ssl port, make sure we're on it,
        // otherwise assume we're already on the right port
        if (sslPort > 0) {
            int portStart = url.indexOf(":", 8) + 1;
            int portEnd   = url.indexOf("/", 8);

            if (portEnd == -1)                              // If their isn't a trailing slash, then the end is the last char
                portEnd = url.length() - 1;

            if (portStart > 0 && portStart < portEnd) {     // If we detected a : before the trailing slash or end of url, delete the port
                url.delete(portStart, portEnd);
            } else {
                url.insert(portEnd, ':');                   // If the url didn't have a port, add in the :
                portStart = portEnd;
            }

            url.insert(portStart, sslPort);   // Insert the right port where it should be
        }

        LogFactory.getLog(ServletUtils.class).debug("redirectOverSSL sending 301: " + url.toString());
        sendPermanentRedirect(response, url.toString());
    }

    /**
     * Redirects the user to the current url over HTTP
     *
     * @param request a HttpServletRequest
     * @param response a HttpServletResponse
     * @param nonSslPort the port Non-SSL requests should be forwarded to
     */
    public static void redirectOverNonSSL(HttpServletRequest request, HttpServletResponse response, int nonSslPort)
        throws ServletException, IOException {

        StringBuffer url = request.getRequestURL();

        // Make sure we're on http
        if (url.charAt(4) == 's')
            url.deleteCharAt(4);

        // If there is a non-ssl port, make sure we're on it,
        // otherwise assume we're already on the right port
        if (nonSslPort > 0) {
            int portStart = url.indexOf(":", 8) + 1;
            int portEnd   = url.indexOf("/", 8);

            if (portEnd == -1)                              // If their isn't a trailing slash, then the end is the last char
                portEnd = url.length() - 1;

            if (portStart > 0 && portStart < portEnd) {     // If we detected a : before the trailing slash or end of url, delete the port
                url.delete(portStart, portEnd);
            } else {
                url.insert(portEnd, ':');                   // If the url didn't have a port, add in the :
                portStart = portEnd;
            }

            url.insert(portStart, nonSslPort);   // Insert the right port where it should be
        }

        LogFactory.getLog(ServletUtils.class).debug("redirectOverSSL sending 301: " + url.toString());
        sendPermanentRedirect(response, url.toString());
    }

    /**
     * Redirects the user to the current url over HTTPS
     *
     * @param request a HttpServletRequest
     * @param response a HttpServletResponse
     * @param nonSslPort the port Non-SSL requests should be forwarded to
     */
    public static void redirectOverNonSSL(HttpServletRequest request, HttpServletResponse response, String urlStr, int nonSslPort)
        throws ServletException, IOException {

        StringBuffer url = new StringBuffer(urlStr);

        // Make sure we're on http
        if (url.charAt(4) == 's')
            url.deleteCharAt(4);

        // If there is a non-ssl port, make sure we're on it,
        // otherwise assume we're already on the right port
        if (nonSslPort > 0) {
            int portStart = url.indexOf(":", 8) + 1;
            int portEnd   = url.indexOf("/", 8);

            if (portEnd == -1)                              // If their isn't a trailing slash, then the end is the last char
                portEnd = url.length() - 1;

            if (portStart > 0 && portStart < portEnd) {     // If we detected a : before the trailing slash or end of url, delete the port
                url.delete(portStart, portEnd);
            } else {
                url.insert(portEnd, ':');                   // If the url didn't have a port, add in the :
                portStart = portEnd;
            }

            url.insert(portStart, nonSslPort);   // Insert the right port where it should be
        }

        LogFactory.getLog(ServletUtils.class).debug("redirectOverSSL sending 301: " + url.toString());
        sendPermanentRedirect(response, url.toString());
    }

    /**
     * Sets the response status code and includes an XML file
     * as the response body with a single root node corresponding
     * to the status code.
     * 
     * @param response
     * @param statusCode
     */
    public static void doXmlStatusCodeError(HttpServletResponse response, int statusCode) 
            throws ServletException, IOException 
    {
        response.setContentType("text/xml");
        response.setStatus(statusCode);
        response.getWriter().println(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<%d/>", statusCode));
    }
    
    /**
     * Expects to compressHTML HTML by replacing line breaks with spaces, and reducing whitespace to single spaces.
     *
     * @param content The string to compressHTML.
     * @return the compressed String, or an empty string if <code>content</code> is null.
     */
    public static String compressHTML(String content) {
        if (StringUtils.isEmpty(content)) return EMPTY;
        return content.replaceAll("\n", SPACE).replaceAll("\\s{2,}", SPACE).replaceAll(" />", "/>");
    }

    /**
     * @param objects An array of objects to check
     * @return true if all elements in the array are non-null or non-empty Strings,
     *         otherwise false
     */
    public static boolean all(Object... objects) {
        for (Object o : objects) {
            if (o == null || (o instanceof String && o.equals(EMPTY))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param objects A Collection of objects to check
     * @return true if all elements in the array are non-null or non-empty Strings,
     *         otherwise false
     */
    public static boolean all(Collection objects) {
        for (Object o : objects) {
            if (o == null || (o instanceof String && o.equals(EMPTY))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param objects An array of objects to check
     * @return true if any element in the array is non-null and a non-empty String,
     *         otherwise false
     */
    public static boolean any(Object... objects) {
        for (Object o : objects) {
            if (o != null || (o instanceof String && !o.equals(EMPTY))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param objects A Collection of objects to check
     * @return true if any element in the array is non-null and a non-empty String,
     *         otherwise false
     */
    public static boolean any(Collection objects) {
        for (Object o : objects) {
            if (o != null || (o instanceof String && !o.equals(EMPTY))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param objects An array of objects to check
     * @return true if no element in the array is non-null and a non-empty String,
     *         otherwise false
     */
    public static boolean none(Object... objects) {
        for (Object o : objects) {
            if (o != null || (o instanceof String && !o.equals(EMPTY))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param objects A Collection of objects to check
     * @return true if no element in the array is non-null and a non-empty String,
     *         otherwise false
     */
    public static boolean none(Collection objects) {
        for (Object o : objects) {
            if (o != null || (o instanceof String && !o.equals(EMPTY))) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Object> T or(T a, T b) {
        return a != null ? a : b;
    }

    /**
     *  @return true if the String argument is null or an empty String, otherwise false
     */
    public static boolean empty(String str) {
        return StringUtils.isEmpty(str);
    }

    /**
     * @return true if the collection argument is null or isEmpty(), otherwise false
     */
    public static boolean empty(Collection c) {
        return c == null || c.isEmpty();
    }

    /**
     * @return true if the array argument is null or length == 0, otherwise false
     */
    public static boolean empty(Object[] array) {
        return ArrayUtils.isEmpty(array);
    }

    /**
     * Returns the argument String surrounded in the argument quore with any pf
     * the argument quotes that were in the string escaped.
     */
    public static String quote(String toQuote, char quote) {
        return String.format("%s%s%s", quote, toQuote.replace(Character.toString(quote), "\\" + quote), quote);
    }

    public static String escapeSingleQuotes(String text) {
        return text.replaceAll("'","\\\\'");
    }

    public static String escapeDoubleQuotes(String text) {
        return text.replaceAll("\"","\\\\\"");
    }

    /**
     * Appends a newline char to the end of the string and returns it.
     */
    public static String line(String str) {
        return String.format("%s\n", str);
    }

    /**
     * Replaces &lt; &gt; &amp; &#39; &quot;
     */
    public static String cleanXSS(String str) {
        return str.replace("&","&amp;")
                  .replace("<","&lt;")
                  .replace(">","&gt;")
                  .replace("'","&#39")
                  .replace("\"","&quot;");
    }

    public static String toCamelCase(String source) {

        if (StringUtils.isEmpty(source)) return EMPTY;
        if (StringUtils.isBlank(source)) return source;

        source = source.replaceAll("-|_|\\.", SPACE);

        String[] parts = source.split(SPACE);

        StringBuilder ret = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                ret.append(parts[i]);
            } else {
                ret.append(capitalizeFirst(parts[i]));
            }
        }

        return ret.toString();
    }

    public static String capitalizeFirst(String source) {

        if (StringUtils.isEmpty(source)) return EMPTY;
        if (StringUtils.isBlank(source)) return source;

        if (source.length() == 1) {
            return source.toUpperCase();
        }

        return source.toUpperCase().charAt(0) + source.substring(1);
    }

    public static String describeRequest(HttpServletRequest req) {

        if (req == null) {
            return EMPTY;
        }

        HttpSession session = null;
        try {
            session = req.getSession();
        } catch (Exception e) {
        }

        StringBuilder body = new StringBuilder();
        body.append("Browser: " + req.getHeader("User-Agent"));

        body.append("\n\nRequest Info");
        body.append("\nRequest URI: " + req.getRequestURI());
        body.append("\nRequest URL: " + req.getRequestURL().toString());
        body.append("\nPath Info: " + req.getPathInfo());
        body.append("\nQuery String: " + req.getQueryString());

        if (session != null) {
            body.append("\n\nSession Info");
            body.append("\nSession ID: " + session.getId());
            body.append("\nSession Created: " + new Date(session.getCreationTime()).toString());
            body.append("\nSession Last Accessed: " + new Date(session.getLastAccessedTime()).toString());
        }

        body.append("\n\nUser Info");
        body.append("\nRemote User: " + req.getRemoteUser());
        body.append("\nUser Principal: " + req.getUserPrincipal());

        body.append("\n\nServer Info");
        String hostname = "", serverInstance = "", ip = "";
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
            serverInstance = System.getProperty("com.sun.aas.instanceName");
            ip = java.net.InetAddress.getLocalHost().getHostAddress();
            body.append("\nInstance: " + serverInstance + " : " + ip + " : " + hostname);
        } catch (Exception e) {
        }

        return body.toString();
    }

    /** Two static methods for use in cookie handling.
     *  Updated to Java 5.
     *  <P>
     *  Taken from Core Servlets and JavaServer Pages 2nd Edition
     *  from Prentice Hall and Sun Microsystems Press,
     *  http://www.coreservlets.com/.
     *  &copy; 2003 Marty Hall; may be freely used or adapted.
     */
    /** Given the request object, a name, and a default value,
     *  this method tries to find the value of the cookie with
     *  the given name. If no cookie matches the name,
     *  the default value is returned.
     */
    public static String getCookieValue(HttpServletRequest request, String cookieName, String defaultValue) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return (cookie.getValue());
                }
            }
        }
        return (defaultValue);
    }

    /** Given the request object and a name, this method tries
     *  to find and return the cookie that has the given name.
     *  If no cookie matches the name, null is returned.
     */
    public static Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return (cookie);
                }
            }
        }
        return (null);
    }

    public static Cookie createCookie(HttpServletResponse response, String name, String value, int expiry, boolean useRootPath) {

        Cookie c = new Cookie(name, value);
        c.setMaxAge(expiry);
        if (useRootPath) {
            c.setPath("/");
        }

        response.addCookie(c);
        return c;
    }

    /**
     * Cookie will last a year
     */
    public static Cookie createCookie(HttpServletResponse response, String name, String value) {
        return createCookie(response, name, value, 60 * 60 * 24 * 356, true);
    }

    /**
     * Removes a cookie
     */
    public static void removeCookie(HttpServletResponse response, String name, boolean useRootPath) {
        createCookie(response, name, EMPTY, 0, useRootPath);
    }

    /**
     * Performes a basic auth authentication on the request comparing the username and password sent in the header
     * to the correct ones correctUsername and correctPassword
     * If no auth is sent or username and password doesn't match it returns false
     * @param request
     * @param correctUsername
     * @param correctPassword
     * @return
     */
    public static boolean basicAuthentication(HttpServletRequest req, String correctUsername, String correctPassword) {

        boolean valid = false;
        
        String userID = null;
        String password = null;

        // Get the Authorization header, if one was supplied

        String authHeader = req.getHeader("Authorization");
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                // We only handle HTTP Basic authentication

                if (basic.equalsIgnoreCase("Basic")) {
                    String credentials = st.nextToken();

                    String userPass;
                    userPass = new String(Base64.decodeBase64(credentials.getBytes()));
                    int p = userPass.indexOf(":");
                    if (p != -1) {
                        userID = userPass.substring(0, p);
                        password = userPass.substring(p + 1);
                        // Validate user ID and password
                        // and set valid true true if valid.
                        if ((userID.equals(correctUsername)) &&
                            (password.equals(correctPassword))) {
                            valid = true;
                        }
                    }
                }
            }
        }

        return valid;
    }
}
