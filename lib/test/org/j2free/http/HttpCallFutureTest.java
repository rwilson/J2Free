/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.j2free.http;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;
import org.apache.commons.httpclient.HttpClient;

import static java.lang.System.out;

/**
 *
 * @author ryan
 */
public class HttpCallFutureTest extends TestCase {

    private final HttpCallFuture instance = new HttpCallFuture("http://www.google.com");
    private final HttpClient client = new HttpClient();
    private final Semaphore semaphore = new Semaphore(1);

    public HttpCallFutureTest(String testName) {
        super(testName);
    }

    public void testRun() throws Throwable {

        out.println("Testing compareTo");
        
        HttpCallFuture high   = new HttpCallFuture("",HttpCallFuture.Priority.HIGH);
        HttpCallFuture low    = new HttpCallFuture("",HttpCallFuture.Priority.LOW);
        HttpCallFuture later  = new HttpCallFuture("",HttpCallFuture.Priority.DEFAULT);
        HttpCallFuture higher = new HttpCallFuture("",HttpCallFuture.Priority.YESTERDAY);

        assertEquals(0, instance.compareTo(instance));
        
        assertEquals(-1, instance.compareTo(high));
        assertEquals(1, high.compareTo(instance));

        assertEquals(-1, high.compareTo(higher));
        assertEquals(1, higher.compareTo(high));

        assertEquals(-1, instance.compareTo(higher));
        assertEquals(1, higher.compareTo(instance));

        assertEquals(1,instance.compareTo(low));
        assertEquals(-1, low.compareTo(instance));
        
        assertEquals(1,instance.compareTo(later));
        assertEquals(-1,later.compareTo(instance));

        out.println("HttpCallFuture.compareTo passed.");
        out.println("Testing HttpCallFuture.run");

        semaphore.acquireUninterruptibly();
        out.println("semaphore acquired");
        instance.initialize(client, semaphore);
        out.println("instance initialized");

        Runnable testThread = new Runnable() {
            public void run() {
                out.println("testThread running");
                try {
                    // this will block
                    assertTrue("Success returned false",instance.success());
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                } catch (ExecutionException e) {
                    fail(e.getMessage());
                }

                HttpCallResult result = instance.get();

                assertNotNull(result);
                assertEquals("Unexpected status code: " + result.getStatusCode(), 200, result.getStatusCode());
                assertNotNull("Response body null!", result.getResponse());
            }
        };
        new Thread(testThread).start();
        out.println("testing run()");
        instance.run();
    }
}
