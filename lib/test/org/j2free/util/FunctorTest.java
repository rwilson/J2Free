package org.j2free.util;

import junit.framework.TestCase;

/**
 *
 * @author Ryan Wilson
 */
public class FunctorTest extends TestCase {
    
    public FunctorTest(String testName) {
        super(testName);
    }

    /**
     * Test of run method, of class Functor.
     */
    public void testRun() {

        final String key = "1/mysong.mid";
        final String fileName = "mysong";

        Functor<String> transformer = new Functor<String>() {
            @Override
            public String run() {
                return key.split("/")[1].replaceFirst(fileName + "-?", "");
            }
        };

        String whatever = transformer.run();
        
    }
}
