/*
 * XmlResponseHandler.java
 *
 * Created on April 1, 2008, 5:32 PM
 *
 * Inspired by, and in very limited part adapted from,
 * FacebookRestClient, Copyright (c) 2007 Facebook, Inc
 *
 * Copyright (c) 2008 Publi.us
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

package org.j2free.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Ryan Wilson 
 */
public class XmlResponseHandler extends ResponseHandler<Document> {
    
    public XmlResponseHandler() {
        super();
    }
    
    public XmlResponseHandler(boolean debug) {
        super(debug);
    }
    
    /**
     * Extracts a String from a T consisting entirely of a String.
     * @return the String
     */
    public static String extractString(Document doc) {
        if (doc == null) {
            return null;
        }
        return doc.getFirstChild().getTextContent();
    }
    
    /**
     * Extracts a String from Document
     * 
     * @param doc the doc
     * @param name the tag name of the node to find
     * @param index the index of the node to get
     * @return the String
     */
    public static String extractString(Document doc, String name, int index) {
        NodeList nodes = doc.getElementsByTagName(name);
        return nodes.getLength() > 0 ? nodes.item(0).getNodeValue() : "";
    }
    
    public static String getAttribute(Document doc, String tagName, String attribute) {
        Node node = doc.getElementsByTagName(tagName).item(0);
        NamedNodeMap attributes = node.getAttributes();
        return (attributes == null) ? "" : attributes.getNamedItem(attribute).toString();
    }
    
    /**
     * Extracts a URL from a document that consists of a URL only.
     * @param doc
     * @return the URL
     */
    public static URL extractURL(Document doc) throws MalformedURLException {
        if (doc == null) {
            return null;
        }
        String url = doc.getFirstChild().getTextContent();
        return (null == url || "".equals(url)) ? null : new URL(url);
    }
    
    /**
     * Extracts an Integer from a doc that consists of an Integer only.
     * 
     * @param doc
     * @return the Integer
     */
    public static int extractInt(Document doc, String name, int index) {
        NodeList nodes = doc.getElementsByTagName(name);
        return nodes.getLength() > 0 ? Integer.parseInt(nodes.item(0).getNodeValue()) : 0;
    }
    
    /**
     * Extracts a Long from a document that consists of a Long only.
     * @param doc
     * @return the Long
     */
    public static Long extractLong(Document doc) {
        if (doc == null) {
            return 0l;
        }
        return Long.parseLong(doc.getFirstChild().getTextContent());
    }
    
    public static boolean extractBoolean(Document doc) {
        if (doc == null)
            return false;
        
        String content = doc.getFirstChild().getTextContent();
        return content.equals("1");
    }
    
    /**
     * Hack...since DOM reads newlines as textnodes we want to strip out those
     * nodes to make it easier to use the tree.
     */
    private static void stripEmptyTextNodes(Node n) {
        NodeList children = n.getChildNodes();
        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node c = children.item(i);
            if (!c.hasChildNodes() && c.getNodeType() == Node.TEXT_NODE &&
                    c.getTextContent().trim().length() == 0) {
                n.removeChild(c);
                i--;
                length--;
                children = n.getChildNodes();
            } else {
                stripEmptyTextNodes(c);
            }
        }
    }
    
    /**
     * Prints out the DOM tree.
     * 
     * @param node the parent node to start printing from
     * @param prefix string to append to output, should not be null
     */
    public static void printDom(Node node, String prefix) {
        String outString = prefix;
        
        if (node.getNodeType() == Node.TEXT_NODE)
            outString += "'" + node.getTextContent().trim() + "'";
        else
            outString += node.getNodeName();
        
        log.debug(outString);
        
        NodeList children = node.getChildNodes();
        
        for (int i = 0; i < children.getLength(); i++)
            XmlResponseHandler.printDom(children.item(i), prefix + "  ");
    }
    
    public static String extractNodeString(Node d) {
        if (d == null) {
            return null;
        }
        return d.getFirstChild().getTextContent();
    }
    
    /**
     * Parses the result of an API call from XML into Java Document
     *
     * @param in an InputStream with the results of a request to the API servers
     * @param method the method
     * @return a Java Object
     * @throws IOException if <code>in</code> is not readable
     */
    protected Document parseCallResult(InputStream in) throws RestClientException, IOException {
        
        Scanner scanner     = new Scanner(new InputStreamReader(in, "UTF-8"));
        StringBuffer buffer = new StringBuffer();
        
        while (scanner.hasNextLine())
            buffer.append(scanner.nextLine());
        
        String data = new String(buffer);
        
        log.debug(data);
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            Document doc = builder.parse(new ByteArrayInputStream(data.getBytes("UTF-8")));
            doc.normalizeDocument();
            stripEmptyTextNodes(doc);
            
            if (debug)
                XmlResponseHandler.printDom(doc,"");

            return doc;
            
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            log.error(e);
            throw new RestClientException(RestClientException.PARSE_ERROR,"Error Parsing XML",data);
        } catch (org.xml.sax.SAXException e) {
            log.error(e);
            throw new IOException("Error Parsing XML");
        }
    }
}
