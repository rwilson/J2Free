/*
 * CollectionExtensions.java
 *
 * Created on March 21, 2008, 10:15 AM
 *
 */

package org.j2free.jsp.el;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.j2free.util.ServletUtils;
import org.j2free.jpa.Controller;

/**
 *
 * @author Ryan Wilson
 */
public class CollectionExtensions {

    /**
     * @deprecated use <tt>size(Collection c)</tt> instead
     */
    public static int getCollectionSize(Collection c) {
        return c == null ? 0 : c.size();
    }

    public static int size(Collection c) {
        return c == null ? 0 : c.size();
    }
    
    /**
     * @deprecated use <tt>contains(Collection c)</tt> instead
     */
    public static boolean collectionContains(Collection c, Object o) {
        return c == null ? false : c.contains(o);
    }

    public static boolean contains(Collection c, Object o) {
        return c == null ? false : c.contains(o);
    }

    public static <T> List<T> shuffle(List<T> list) {
        if (list != null && !list.isEmpty()) 
            Collections.shuffle(list);
        
        return list;
    }
    
    public static boolean isEmpty(Collection c) {
        return c == null ? true : c.isEmpty();
    }
    
    public static <T extends Object> List<T> filter(Collection c, String filter) {
        return Controller.get().filter(c,filter);
    }

    public static <T extends Object> List<T> filterLimited(Collection c, String filter, int start, int limit) {
        return Controller.get().filter(c,filter,start,limit);
    }

    public static String join(Collection c, String delimiter) {
        return ServletUtils.join(c, delimiter);
    }

    public static List sort(Collection c) {
        List list = new ArrayList(c);
        Collections.sort(list);
        return list;
    }
}
