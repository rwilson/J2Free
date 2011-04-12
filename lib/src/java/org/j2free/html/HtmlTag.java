/*
 * HtmlTag.java
 *
 * Created on November 23, 2008, 1:43 AM
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
package org.j2free.html;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan Wilson 
 */
public class HtmlTag {
    
    private final String OPEN       = "<";
    private final String CLOSE      = ">";
    private final String SELF_CLOSE = " />";
    private final String END_OPEN   = "</";
    
    private final String SPACE      = " ";
    private final String ATT_CLOSE  = "\"";
    private final String ATT_OPEN   = "=\"";
    
    /**
     *
     */
    protected String  body;
    /**
     *
     */
    protected boolean selfClosing;
    
    private String tagName;
    private HashMap<String,String> attributes;
    
    /**
     * 
     * @param tagName
     */
    public HtmlTag(String tagName)
    {
        this(tagName,"",false);
    }
    
    /**
     * 
     * @param tagName
     * @param body
     */
    public HtmlTag(String tagName, String body)
    {
        this(tagName,body,false);
    }
    
    /**
     * 
     * @param tagName
     * @param selfClosing
     */
    public HtmlTag(String tagName, boolean selfClosing)
    {
        this(tagName,null,selfClosing);
    }
    
    /**
     * 
     * @param tagName
     * @param body
     * @param selfClosing
     */
    public HtmlTag(String tagName, String body, boolean selfClosing)
    {
        this.tagName     = tagName;
        this.body        = body;
        this.selfClosing = selfClosing;
        
        attributes = new HashMap<String,String>();
    }
    
    /**
     * 
     * @param key
     * @param value
     * @return
     */
    public HtmlTag setAttribute(String key, String value)
    {
        attributes.put(key,value);
        return this;
    }
    
    /**
     * 
     * @param body
     * @return
     */
    public HtmlTag setBody(String body)
    {
        this.body = body;
        return this;
    }
    
    /**
     * 
     * @param tagName
     * @return
     */
    public HtmlTag setTagName(String tagName)
    {
        this.tagName = tagName;
        return this;
    }
    
    /**
     * 
     * @param selfClosing
     * @return
     */
    public HtmlTag setSelfClosing(boolean selfClosing)
    {
        this.selfClosing = selfClosing;
        return this;
    }
    
    /**
     * 
     * @param title
     * @return
     */
    public HtmlTag setTitle(String title)
    {
        setAttribute("title",title);
        return this;
    }
    
    /**
     * 
     * @param alt
     * @return
     */
    public HtmlTag setAlt(String alt)
    {
        setAttribute("alt",alt);
        return this;
    }
    
    /**
     * 
     * @param style
     * @return
     */
    public HtmlTag setStyle(String style)
    {
        setAttribute("style",style);
        return this;
    }
    
    /**
     * 
     * @param className
     * @return
     */
    public HtmlTag setClass(String className)
    {
        setAttribute("class",className);
        return this;
    }
    
    /**
     * 
     * @param id
     * @return
     */
    public HtmlTag setId(String id)
    {
        setAttribute("id",id);
        return this;
    }
    
    /**
     * 
     * @return
     */
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(OPEN + tagName);
        
        for (Map.Entry<String,String> attribute : attributes.entrySet())
            builder.append(SPACE + attribute.getKey() + ATT_OPEN + attribute.getValue() + ATT_CLOSE);
        
        if (selfClosing)
            builder.append(SELF_CLOSE);
        else
            builder.append(CLOSE + body + END_OPEN + tagName + CLOSE);
        
        return builder.toString();
    }
    
    /**
     * 
     * @return
     */
    public static String br()
    {
        return br(1);
    }
    
    /**
     * 
     * @param count
     * @return
     */
    public static String br(int count)
    {
        HtmlTag br = new HtmlTag("br",true);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++)
            builder.append(br.toString());

        return builder.toString();
    }

    /**
     * 
     */
    public static class a extends HtmlTag
    {
        
        /**
         * 
         * @param body
         */
        public a(String body)
        {
            super("a",body,false);
        }
        
        /**
         * 
         * @param body
         * @param href
         */
        public a(String body, String href)
        {
            super("a",body,false);
            setAttribute("href",href);
        }
        
        /**
         * 
         * @param href
         * @return
         */
        public a setHref(String href)
        {
            setAttribute("href",href);
            return this;
        }
    }
    
    /**
     * 
     */
    public static class img extends HtmlTag
    {
        
        /**
         * 
         * @param src
         */
        public img(String src)
        {
            super("img");
            selfClosing = true;
            setAttribute("src",src);
        }
        
        /**
         * 
         * @param src
         */
        public void setSrc(String src)
        {
            setAttribute("src",src);
        }
    }
    
    /**
     * 
     */
    public static class table extends HtmlTag
    {
        
        private List<tr> rows;
        
        /**
         * 
         */
        public table()
        {
            super("table",false);
            this.rows = new LinkedList<tr>();
        }
        
        /**
         * 
         * @return
         */
        public tr row()
        {
            tr row = new tr(this);
            rows.add(row);
            return row;
        }
        
        /**
         *
         * @return
         */
        @Override
        public String toString() {
            
            StringBuilder builder = new StringBuilder();
            for (tr row : rows)
                builder.append(row.toString());
            
            body = builder.toString();
            return super.toString();
        }
    }
    
    /**
     * 
     */
    public static class tr extends HtmlTag
    {
        
        private List<td> cells;
        private table parent;
        
        private tr(table parent) {
            super("tr",false);
            
            this.parent = parent;
            this.cells  = new LinkedList<td>();
        }
        
        /**
         * 
         * @param body
         * @return
         */
        public td td(String body)
        {
            td cell = new td(this,body);
            cells.add(cell);
            return cell;
        }
        
        /**
         * 
         * @param body
         * @return
         */
        public th th(String body)
        {
            th cell = new th(this,body);
            cells.add(cell);
            return cell;
        }
        
        /**
         * 
         * @return
         */
        public table end()
        {
            return parent;
        }
        
        /**
         *
         * @return
         */
        @Override
        public String toString() {
            
            StringBuilder builder = new StringBuilder();
            for (td cell : cells)
                builder.append(cell.toString());
            
            body = builder.toString();
            return super.toString();
        }
    }
    
    /**
     * 
     */
    public static class td extends HtmlTag
    {
        
        private tr parent;
        
        /**
         * 
         * @param parent
         * @param body
         */
        public td(tr parent, String body)
        {
            this("td",parent,body);
        }
        
        /**
         * 
         * @param cellType
         * @param parent
         * @param body
         */
        public td(String cellType, tr parent, String body)
        {
            super(cellType,body,false);
            this.parent = parent;
        }
        
        /**
         * 
         * @param colspan
         * @return
         */
        public td colspan(int colspan)
        {
            setAttribute("colspan","" + colspan);
            return this;
        }
        
        /**
         * 
         * @param rowspan
         * @return
         */
        public td rowspan(int rowspan)
        {
            setAttribute("rowspan","" + rowspan);
            return this;
        }
        
        /**
         * 
         * @return
         */
        public tr end()
        {
            return parent;
        }
        
        /**
         *
         * @param style
         * @return
         */
        @Override
        public td setStyle(String style) {
            super.setStyle(style);
            return this;
        }
    }

    /**
     * 
     */
    public static class th extends td
    {

        /**
         * 
         * @param parent
         * @param body
         */
        public th(tr parent,String body)
        {
            super("th",parent,body);
        }
    }
    
    /* Testing
    public static void main(String[] args) {
        System.out.println(
        new HtmlTag.table()
           .row()
               .th("&nbsp;")
               .th("Points")
               .th("Percent Hit")
               .th("Max Streak")
           .end()
           .row()
               .td("Challenger")
               .td("100000")
               .td("90%")
               .td("200")
           .end()
           .row()
               .td("Challengee")
               .td("102000")
               .td("91%")
               .td("205")
           .end()
           // These attributes are on table, so they have to be after end (which returns the table, not the tr)
           .setAttribute("cellpadding","0")
           .setAttribute("cellspacing","5")
           .setAttribute("border","0")
      );
        
        System.out.println(
            new HtmlTag.a("Click Here","http://www.jamlegend.com").setTitle("Click here").setAlt("a link").setStyle("font-size:14px;color:#666;")
        );
    }
    */
}