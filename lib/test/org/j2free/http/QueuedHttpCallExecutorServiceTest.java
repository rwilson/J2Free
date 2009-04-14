/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.j2free.http;

import java.util.concurrent.CountDownLatch;
import junit.framework.TestCase;

/**
 *
 * @author ryan
 */
public class QueuedHttpCallExecutorServiceTest extends TestCase {

    private static final int N_THREADS = 1;

    public void testSubmit() throws InterruptedException {

        final String[] urls = new String[] {
            "http://www.google.com",
            "http://www.amazon.com",
            "http://www.fliggo.com",
            "http://www.jamlegend.com",
            "http://www.scoopler.com"
        };

        final QueuedHttpCallExecutorService service = new QueuedHttpCallExecutorService();

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate   = new CountDownLatch(N_THREADS);

        for (int i = 0; i < N_THREADS; i++) {
            Thread thread = new Thread("Thread-" + i) {
                public void run() {
                    try {

                        int rand = ((Double)Math.floor(Math.random() * urls.length)).intValue();
                        HttpCallFuture future = new HttpCallFuture(urls[rand]);

                        startGate.await();
                        try {
                            long s = System.currentTimeMillis();
                            service.submit(future);
                            HttpCallResult result = future.getResult();
                            long e = System.currentTimeMillis();
                            System.out.println(getName() + " finished fetching " + future.getUrl() + " in " + (e - s) + "ms [resultStatus=" + result.getStatusCode() + "]");
                        } finally {
                            endGate.countDown();
                        }
                    } catch (InterruptedException ie) { }
                }
            };
            thread.start();
        }

        long start = System.currentTimeMillis();
        startGate.countDown();
        endGate.await();
        long end = System.currentTimeMillis();

        System.out.println("Completed in " + (end - start) + "ms");
    }

}
