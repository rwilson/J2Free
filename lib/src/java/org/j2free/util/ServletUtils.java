/*
 * ServletUtils.java
 *
 * Created on September 14, 2007, 4:06 AM
 *
 */
package org.j2free.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.j2free.jpa.Controller;
import org.j2free.security.SecurityUtils;

import static org.j2free.util.Constants.*;

/**
 *
 * @author ryan
 */
public class ServletUtils {

    public static int getIntParameter(HttpServletRequest req, String paramName) {
        return getIntParameter(req, paramName, -1);
    }

    public static int getIntParameter(HttpServletRequest req, String paramName, int defaultValue) {
        int i = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                i = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static short getShortParameter(HttpServletRequest req, String paramName) {
        short s = -1;
        return getShortParameter(req, paramName, s);
    }

    public static short getShortParameter(HttpServletRequest req, String paramName, short defaultValue) {
        short i = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                i = Short.parseShort(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static long getLongParameter(HttpServletRequest req, String paramName, long defaultValue) {
        long l = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                l = Long.parseLong(s);
            } catch (Exception e) {
            }
        }
        return l;
    }

    public static float getFloatParameter(HttpServletRequest req, String paramName, float defaultValue) {
        float i = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                i = Float.parseFloat(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static double getDoubleParameter(HttpServletRequest req, String paramName, double defaultValue) {
        double i = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                i = Double.parseDouble(s);
            } catch (Exception e) {
            }
        }
        return i;
    }

    public static boolean getBooleanParameter(HttpServletRequest req, String paramName, boolean defaultValue) {
        boolean b = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                b = Boolean.parseBoolean(s);
            } catch (Exception e0) {
            }
        }
        return b;
    }

    public static java.sql.Date getDateParameter(HttpServletRequest req, String paramName, java.sql.Date defaultValue) {
        java.sql.Date d = defaultValue;
        String s = req.getParameter(paramName);
        if (s != null) {
            try {
                d = java.sql.Date.valueOf(s);
            } catch (Exception e) {
            }
        }
        return d;
    }

    public static String getStringParameter(HttpServletRequest req, String paramName) {
        return getStringParameter(req, paramName, "");
    }

    public static String getStringParameter(HttpServletRequest req, String paramName, String defaultValue) {
        String s = req.getParameter(paramName);
        if (s == null) {
            s = defaultValue;
        }
        return s == null ? s : s.trim();
    }

    public static String[] getStringArrayParameter(HttpServletRequest req, String paramName, String delimiter) {
        return getStringArrayParameter(req, paramName, delimiter, new String[0]);
    }

    public static String[] getStringArrayParameter(HttpServletRequest req, String paramName, String delimiter,
                                                   String[] defaultValue) {
        String s = req.getParameter(paramName);

        if (s == null) {
            return defaultValue;
        }

        return splitAndTrim(s, delimiter);
    }

    /**
     * @author  Ryan
     * @description Prints the stack trace of a Throwable object to a String and
     *              returns the String.
     * @param   t - the Throwable item
     */
    public static String throwableToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    public static String formHyperlink(String href, String text) {
        return "<a href=\"" + href + "\">" + text + "</a>";
    }

    /** 
     * @param   words to join
     * @param   separator, the separator to put between words
     * @return  String of words concatonated with the separator
     */
    public static String join(String[] words, String separator) {

        if (words == null || words.length == 0)
            return "";


        String ret = "";
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                ret += separator;
            }
            ret += words[i];
        }
        return ret;
    }

    /**
     * @param   joined = string to split
     * @param   separator = delimiter to split by
     * @return  Array of Strings
     */
    public static String[] splitAndTrim(String joined, String separator) {
        if (joined == null || joined.equals("")) {
            return new String[0];
        }

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

        if (words == null || words.isEmpty())
            return "";

        String ret = "";
        Iterator<T> itr = words.iterator();
        int i = 0;
        while (itr.hasNext()) {
            if (i > 0) {
                ret += separator;
            }
            ret += itr.next().toString();
            i++;
        }
        return ret;
    }

    /**
     * Returns true if the parameter string prior to the "sig" parameter match the "sig" parameter when combined with the key and hashed
     * @param a request object
     * @param a secret key
     */
    public static boolean isAuthenticatedRequest(HttpServletRequest request, String key) {
        String query = request.getQueryString();
        if (query != null) {
            int pos = query.lastIndexOf("&sig=");
            if (pos != -1) {
                query = query.replaceFirst("&sig=.{40}", "");
                query = query + key;
                String sig = request.getParameter("sig");
                try {
                    return SecurityUtils.SHA1(query).equals(sig);
                } catch (Exception ex) {
                    return false;
                }
            }
        }
        return false;
    }

    public static String getCaptchaCorrect(HttpServletRequest req) {
        return (String) (req.getSession(true).getAttribute(nl.captcha.servlet.Constants.SIMPLE_CAPCHA_SESSION_KEY));
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

            if (compress)
                responseString = compress(responseString);

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

    /**
     * Expects to compress HTML by replacing line breaks with spaces, and reducing whitespace to single spaces.
     *
     * @param content The string to compress.
     * @return the compressed String, or an empty string if <code>content</code> is null.
     */
    public static String compress(String content) {
        if (content == null)
            return "";

        return content.replaceAll("\n", " ").replaceAll("\\s{2,}", " ").replaceAll(" />", "/>");
    }

    // Will return the controller if using the default
    public static Controller getController(HttpServletRequest request) {
        return (Controller) request.getAttribute(Controller.ATTRIBUTE_KEY);
    }

    // Will return the controller if using a subclass
    public static <T extends Controller> T getController(HttpServletRequest request, Class<T> controllerClass) {
        return (T) request.getAttribute(Controller.ATTRIBUTE_KEY);
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
     *  @return true if the String argument is null or an empty String, otherwise false
     */
    public static boolean empty(String str) {
        return str == null || str.equals(EMPTY);
    }

    public static String toCamelCase(String source) {

        if (source == null || source.equals("")) {
            return "";
        }

        source = source.replaceAll("-|_|\\.", " ");

        String[] parts = source.split(" ");

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                result.append(parts[i]);
            } else {
                result.append(capitalizeFirst(parts[i]));
            }
        }

        return result.toString();
    }

    public static String capitalizeFirst(String source) {
        if (source == null || source.equals("")) {
            return "";
        }

        if (source.length() == 1) {
            return source.toUpperCase();
        }

        return source.toUpperCase().charAt(0) + source.substring(1);
    }

    public static String describeRequest(HttpServletRequest req) {

        if (req == null) {
            return "";
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
    public static String getCookieValue(HttpServletRequest request,
                                        String cookieName,
                                        String defaultValue) {
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
    public static Cookie getCookie(HttpServletRequest request,
                                   String cookieName) {
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

    public static Cookie createCookie(HttpServletResponse response, String name, String value, int maxAge,
                                      boolean useRootPath) {
        Cookie c = new Cookie(name, value);
        c.setMaxAge(maxAge);
        if (useRootPath) {
            c.setPath("/");
        }

        response.addCookie(c);

        return c;
    }

    /* Cookie will last a year
     */
    public static Cookie createCookie(HttpServletResponse response, String name, String value) {
        return createCookie(response, name, value, 60 * 60 * 24 * 356, true);
    }
}