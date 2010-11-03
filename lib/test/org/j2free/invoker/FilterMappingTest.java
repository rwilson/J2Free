
package org.j2free.invoker;

import junit.framework.TestCase;

/**
 *
 * @author ryan
 */
public class FilterMappingTest extends TestCase {
    
    public FilterMappingTest(String testName) {
        super(testName);
    }

    /**
     * Test of compareTo method, of class FilterMapping.
     */
    public void testCompareTo() {

        FilterMapping m0 = new FilterMapping(null, "/*", false, 0);
        FilterMapping m1 = new FilterMapping(null, "/test/*", false, 0);
        FilterMapping m2 = new FilterMapping(null, "/blah/*", false, 0);
        FilterMapping m3 = new FilterMapping(null, "/test/blah/*", false, 0);

        assertEquals(m0.compareTo(m1), -1);
        assertEquals(m0.compareTo(m2), -1);
        assertEquals(m0.compareTo(m3), -1);

        assertEquals(m1.compareTo(m0),  1);
        assertEquals(m1.compareTo(m2),  0);
        assertEquals(m1.compareTo(m3), -1);

        assertEquals(m2.compareTo(m0),  1);
        assertEquals(m2.compareTo(m1),  0);
        assertEquals(m2.compareTo(m3), -1);

        assertEquals(m3.compareTo(m0), 1);
        assertEquals(m3.compareTo(m1), 1);
        assertEquals(m3.compareTo(m2), 1);
    }

    /**
     * Test of appliesTo method, of class FilterMapping.
     */
    public void testAppliesTo() {

        FilterMapping m0 = new FilterMapping(null, "/*", false, 0);
        FilterMapping m1 = new FilterMapping(null, "/test/*", false, 0);
        FilterMapping m2 = new FilterMapping(null, "/test/blah/*", false, 0);

        assertNotNull(m0);
        assertNotNull(m1);
        assertNotNull(m2);

        assertNotNull(m0.path);
        assertNotNull(m1.path);
        assertNotNull(m2.path);

        assertTrue(m0.appliesTo("/a"));
        assertTrue(m0.appliesTo("/a/b"));
        assertTrue(m0.appliesTo("/"));
        assertTrue(m0.appliesTo("/index.jsp"));

        assertFalse(m1.appliesTo("/a"));
        assertFalse(m1.appliesTo("/a/b"));
        assertFalse(m1.appliesTo("/"));
        assertTrue(m1.appliesTo("/test/a"));
        assertTrue(m1.appliesTo("/test/a/b"));

        assertFalse(m2.appliesTo("/a"));
        assertFalse(m2.appliesTo("/a/b"));
        assertFalse(m2.appliesTo("/"));
        assertFalse(m2.appliesTo("/test/a"));
        assertFalse(m2.appliesTo("/test/a/b"));

        assertTrue(m2.appliesTo("/test/blah/"));
        assertTrue(m2.appliesTo("/test/blah/a"));
        assertTrue(m2.appliesTo("/test/blah/a/b"));
    }
}
