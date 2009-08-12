package org.j2free.email;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.Session;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;

import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;

import static java.lang.System.out;

/**
 *
 * @author Ryan Wilson
 */
public class EmailServiceTest extends TestCase {
    
    private static final int N_THREADS = 20;
    private static final String KEY = "test-emailservice-key";
    private static final KeyValuePair<String,String> FROM = new KeyValuePair<String,String>("Test","from@test.com");

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


    public void testSend() throws InterruptedException {

        Properties props = System.getProperties();
        Session session = Session.getInstance(props);

        EmailService service = new EmailService(session);
        EmailService.initialize(1,1,0);
        EmailService.registerInstance(KEY, service);
        EmailService.enableDummyMode();

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate   = new CountDownLatch(N_THREADS);
        
        for (int i = 0; i < N_THREADS; i++) {
            Thread thread = new Thread("Thread-" + i) {
                @Override
                public void run() {
                    try {

                        startGate.await();

                        for (int i = 0; i < 100; i++) {
                            EmailService.getInstance(KEY)
                                        .sendPlain(
                                            FROM,
                                            "to@test.com",
                                            getName() + " #" + i,
                                            "Body",
                                            Priority.values()[((Double)(Math.floor(Priority.values().length * Math.random()))).intValue()]
                                        );
                        }

                    } catch (Exception e) {
                        out.println(getName() + " failed with an excepiton");
                        e.printStackTrace();
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

        out.println("All messages submitted in " + (end - start) + "ms");

        boolean normal = EmailService.shutdown(30, TimeUnit.SECONDS);
        assertTrue(normal);
    }
}