/*
 * CollectionExtensions.java
 *
 * Created on March 21, 2008, 10:15 AM
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
package org.j2free.jsp.el;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import org.j2free.util.ServletUtils;
import org.j2free.jpa.Controller;

/**
 * @author Ryan Wilson
 */
public class CollectionExtensions {

    /**
     * @param c 
     * @return
     * @deprecated use <tt>size(Collection c)</tt> instead
     */
    public static int getCollectionSize(Collection c) {
        return c == null ? 0 : c.size();
    }

    /**
     * 
     * @param c
     * @return
     */
    public static int size(Collection c)
    {
        return c == null ? 0 : c.size();
    }
    
    /**
     * @param c 
     * @param o
     * @return
     * @deprecated use <tt>contains(Collection c)</tt> instead
     */
    public static boolean collectionContains(Collection c, Object o) {
        return c == null ? false : c.contains(o);
    }

    /**
     * 
     * @param c
     * @param o
     * @return
     */
    public static boolean contains(Collection c, Object o)
    {
        return c == null ? false : c.contains(o);
    }

    /**
     * 
     * @param <T>
     * @param list
     * @return
     */
    public static <T> List<T> shuffle(List<T> list)
    {
        if (list != null && !list.isEmpty()) 
            Collections.shuffle(list);
        
        return list;
    }
    
    /**
     * 
     * @param c
     * @return
     */
    public static boolean isEmpty(Collection c)
    {
        return c == null ? true : c.isEmpty();
    }
    
    /**
     * 
     * @param <T>
     * @param c
     * @param filter
     * @return
     * @throws ServletException
     */
    public static <T extends Object> List<T> filter(Collection c, String filter) throws ServletException
    {

        boolean release = false;
        try {
            
            Controller controller = Controller.get(false);
            if (controller == null) {
                controller = Controller.get();
                release = true;
            }
            
            return controller.filter(c,filter);
            
        } finally {
            if (release) {
                Controller.release();
            }
        }
    }

    /**
     *
     * @param <T>
     * @param c
     * @param filter
     * @param start
     * @param limit
     * @return
     * @throws ServletException
     */
    public static <T extends Object> List<T> filterLimited(Collection c, String filter, int start, int limit)
            throws ServletException {

        boolean release = false;
        try {
            
            Controller controller = Controller.get(false);
            if (controller == null) {
                controller = Controller.get();
                release = true;
            }
            
            return controller.filter(c,filter,start,limit);

        } finally {
            if (release) {
                Controller.release();
            }
        }
    }

    /**
     * 
     * @param c
     * @param delimiter
     * @return
     */
    public static String join(Collection c, String delimiter)
    {
        return ServletUtils.join(c, delimiter);
    }

    /**
     * 
     * @param c
     * @return
     */
    public static List sort(Collection c)
    {
        List list = new ArrayList(c);
        Collections.sort(list);
        return list;
    }
}
