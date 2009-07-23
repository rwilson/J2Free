package org.j2free.http;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;
import junit.framework.TestCase;

import org.j2free.http.QueuedHttpCallService.Report;
import org.j2free.util.concurrent.CountingSet;

import static java.lang.System.out;

/**
 *
 * @author Ryan Wilson
 */
public class QueuedHttpCallServiceTest extends TestCase {

    private static final int N_THREADS = 100;

    public void testSubmit() throws InterruptedException {

        try {
            Class.forName("org.j2free.http.QueuedHttpCallService");
        } catch (ClassNotFoundException e) {
            fail("QueuedHttpCallService could not be called!");
        }

        final String[] urls = new String[] {
            "http://www.fliggo.com",
            "http://www.scoopler.com",
            "http://www.google.com"
        };

        final CountDownLatch                endGate = new CountDownLatch(N_THREADS);
        final CountingSet<String>           counter = new CountingSet<String>();
        final ConcurrentLinkedQueue<Report> reports = new ConcurrentLinkedQueue<Report>();

        final AtomicLong totalTime = new AtomicLong(0);
        final AtomicLong waitTime  = new AtomicLong(0);

        QueuedHttpCallService.enable(-1, 5, 30, 30);

        Thread reporter = new Thread("ReportThread") {
            @Override
            public void run() {
                for (;;) {
                    try {
                        reports.add(QueuedHttpCallService.reportStatus());
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        return;
                    } catch (Exception e) {
                        out.println("Error generating report: " + e);
                    }
                }
            }
        };

        reporter.start();

        for (int i = 0; i < N_THREADS; i++) {
            Thread thread = new Thread("Thread-" + i) {
                @Override
                public void run() {
                    try {

                        long s = System.currentTimeMillis();

                        int rand = ((Double)Math.floor(Math.random() * urls.length)).intValue();

                        HttpCallTask task = new HttpCallTask(urls[rand]);

                        HttpCallFuture future = QueuedHttpCallService.submit(task);

                        long c = System.currentTimeMillis();
                        HttpCallResult result = future.get();

                        long e = System.currentTimeMillis();

                        waitTime.addAndGet(e - c);
                        totalTime.addAndGet(e - s);

                        counter.add(task.url);

                        //out.println(getName() + " finished fetching " + task.url + " [resultStatus=" + result.getStatusCode() + ",statusLine=" + result.getStatusLine().toString() + "]");
                        //out.print(result.getResponse());

                    } catch (Exception e) {
                        out.println(getName() + " failed with an excepiton: " + e.getMessage());
                    } finally {
                        endGate.countDown();
                    }
                }
            };
            thread.start();
        }

        endGate.await();

        Thread.sleep(30000);

        reporter.interrupt();
        
        out.println("Completed in " + totalTime.get() + "ms, " + waitTime.get() + "ms was waiting");

        String[] urlHits = counter.toArray(new String[counter.size()]);
        for (String url : urlHits) {
            out.println(url + " fetched " + counter.getAddCount(url) + " times");
        }

        for (Report report : reports) {
            out.println("[ " +
                    "current: "  + report.getCurrentPoolSize()    + ", " +
                    "largest: "  + report.getLargestPoolSize()    + ", " +
                    "max: "      + report.getMaxPoolSize()        + ", " +
                    "active: "   + report.getActiveThreadCount()  + ", " +
                    "total: "    + report.getTotalTaskCount()     + ", " +
                    "complete: " + report.getCompletedTaskCount() + " ]"
                );
        }

        boolean normal = QueuedHttpCallService.shutdown(30, TimeUnit.SECONDS);
        assertTrue(normal);
    }

}
