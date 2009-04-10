/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.j2free.http;

import junit.framework.TestCase;

/**
 *
 * @author ryan
 */
public class QueuedHttpCallExecutorServiceTest extends TestCase {
    
    public QueuedHttpCallExecutorServiceTest(String testName) {
        super(testName);
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
     * Test of submit method, of class QueuedHttpCallExecutorService.
     */
    public void testSubmit() {
        System.out.println("submit");
        HttpCallFuture task = null;
        QueuedHttpCallExecutorService instance = new QueuedHttpCallExecutorService();
        instance.submit(task);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
