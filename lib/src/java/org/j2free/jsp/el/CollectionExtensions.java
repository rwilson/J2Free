/*
 * CollectionExtensions.java
 *
 * Created on March 21, 2008, 10:15 AM
 *
 */

package org.j2free.jsp.el;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.j2free.etc.ServletUtils;
import org.j2free.jpa.Controller;

/**
 *
 * @author ryan
 */
public class CollectionExtensions {
    
    public static int getCollectionSize(Collection c) {
        return c == null ? 0 : c.size();
    }
    
    public static boolean collectionContains(Collection c, Object o) {
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
    
    public static <T extends Object> List<T> filter(Controller controller, Collection c, String filter) {
        return controller.filter(c,filter);
    }

    public static <T extends Object> List<T> filterLimited(Controller controller, Collection c, String filter, int start, int limit) {
        return controller.filter(c,filter,start,limit);
    }

    public static String join(Collection c, String delimiter) {
        return ServletUtils.join(c, delimiter);
    }
}
