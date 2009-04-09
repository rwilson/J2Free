/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.j2free.http;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author ryan
 */
public class QueuedHttpCallExecutorServiceTest extends TestCase {
    
    public QueuedHttpCallExecutorServiceTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(QueuedHttpCallExecutorServiceTest.class);
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of start method, of class QueuedHttpCallExecutorService.
     */
    public void testStart() {
        System.out.println("start");
        QueuedHttpCallExecutorService.start();
        assertTrue("QueuedHttpCallExecutorService failed to start", QueuedHttpCallExecutorService.isRunning());
    }

    /**
     * Test of enqueue method, of class QueuedHttpCallExecutorService.
     */
    public void testEnqueue() {
        System.out.println("enqueue");
        HttpCallFuture future = new HttpCallFuture("http://www.google.com");
        boolean result = QueuedHttpCallExecutorService.enqueue(future);
        assertEquals("Failed to enqueue future", true, result);
    }

}
