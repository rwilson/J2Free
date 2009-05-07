package org.j2free.http;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.j2free.util.concurrent.CountingSet;

import static java.lang.System.out;

/**
 *
 * @author ryan
 */
public class QueuedHttpCallServiceTest extends TestCase {

    private static final int N_THREADS = 1000;

    public void testSubmit() throws InterruptedException {

        try {
            Class.forName("org.j2free.http.QueuedHttpCallService");
        } catch (ClassNotFoundException e) {
            fail("QueuedHttpCallService could not be called!");
        }

        final String[] urls = new String[] {
            "http://www.fliggo.com",
            "http://www.scoopler.com"
        };

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate   = new CountDownLatch(N_THREADS);

        final CountingSet<String> counter = new CountingSet<String>();

        for (int i = 0; i < N_THREADS; i++) {
            Thread thread = new Thread("Thread-" + i) {
                @Override
                public void run() {
                    try {

                        int rand = ((Double)Math.floor(Math.random() * urls.length)).intValue();
                        
                        HttpCallTask task = new HttpCallTask(urls[rand],true);

                        startGate.await();

                        HttpCallFuture future = QueuedHttpCallService.submit(task);

                        HttpCallResult result = future.get();

                        counter.add(task.url);


                        out.println(getName() + " finished fetching " + task.url + " [resultStatus=" + result.getStatusCode() + "]");
                        
                    } catch (Exception e) {
                        out.println(getName() + " failed with an excepiton: " + e.getMessage());
                    } finally {
                        endGate.countDown();
                    }
                }
            };
            thread.start();
        }

        long start = System.currentTimeMillis();
        startGate.countDown();
        endGate.await(30,TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        out.println("Completed in " + (end - start) + "ms");

        String[] urlHits = counter.toArray(new String[counter.size()]);
        for (String url : urlHits) {
            out.println(url + " fetched " + counter.getAddCount(url) + " times");
        }

        boolean normal = QueuedHttpCallService.shutdown(30, TimeUnit.SECONDS);
        assertTrue(normal);
    }

}
