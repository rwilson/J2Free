package org.j2free.http;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;
import junit.framework.TestCase;

import org.apache.commons.httpclient.Header;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.j2free.util.concurrent.CountingSet;

import static java.lang.System.out;

/**
 *
 * @author Ryan Wilson
 */
public class QueuedHttpCallServiceTest extends TestCase {

    private static final int N_THREADS = 100;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Layout layout = new PatternLayout("%c{1} %x - %m%n");
        ConsoleAppender appender = new ConsoleAppender(layout);
        BasicConfigurator.configure(appender);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        BasicConfigurator.resetConfiguration();
    }

    public void testSubmit() throws InterruptedException {

        Logger httpLogger = Logger.getLogger("org.apache.commons.httpclient");
        httpLogger.setLevel(Level.OFF);

        try {
            Class.forName("org.j2free.http.QueuedHttpCallService");
        } catch (ClassNotFoundException e) {
            fail("QueuedHttpCallService could not be called!");
        }

        final String[] urls = new String[] {
            "http://vidly.com",
            "http://www.scoopler.com",
            "http://www.google.com"
        };

        final CountDownLatch                endGate = new CountDownLatch(N_THREADS);
        final CountingSet<String>           counter = new CountingSet<String>();
        final ConcurrentLinkedQueue<HttpServiceReport> reports = new ConcurrentLinkedQueue<HttpServiceReport>();

        final AtomicLong totalTime = new AtomicLong(0);
        final AtomicLong waitTime  = new AtomicLong(0);

        final QueuedHttpCallService service = new QueuedHttpCallService(5, -1, 60, 30, 30);

        Thread reporter = new Thread("ReportThread") {
            @Override
            public void run() {
                for (;;) {
                    try {
                        reports.add(service.reportStatus());
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
                        task.addRequestHeader(new Header("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"));
                        task.addRequestHeader(new Header("Accept-Language", "en-us"));
                        task.addRequestHeader(new Header("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_2; en-us) AppleWebKit/531.21.8 (KHTML, like Gecko) Version/4.0.4 Safari/531.21.10"));

                        Future<HttpCallResult> future = service.submit(task);

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

        Thread.sleep(5000);

        reporter.interrupt();
        
        out.println("Completed in " + totalTime.get() + "ms, " + waitTime.get() + "ms was waiting");

        String[] urlHits = counter.toArray(new String[counter.size()]);
        for (String url : urlHits) {
            out.println(url + " fetched " + counter.getAddCount(url) + " times");
        }

        for (HttpServiceReport report : reports) {
            out.println("[ " +
                    "current: "  + report.getCurrentPoolSize()    + ", " +
                    "largest: "  + report.getLargestPoolSize()    + ", " +
                    "max: "      + report.getMaxPoolSize()        + ", " +
                    "active: "   + report.getActiveThreadCount()  + ", " +
                    "total: "    + report.getTotalTaskCount()     + ", " +
                    "complete: " + report.getCompletedTaskCount() + " ]"
                );
        }

        boolean normal = service.shutdown(30, TimeUnit.SECONDS);
        assertTrue(normal);
    }

}
