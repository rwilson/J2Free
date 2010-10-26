package org.j2free.email;

import java.util.List;
import java.util.Properties;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.mail.Session;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;

import static java.lang.System.out;

/**
 *
 * @author Ryan Wilson
 */
public class EmailServiceTest extends TestCase
{
    private final int N_THREADS = 20;
    private final KeyValuePair<String,String> FROM = new KeyValuePair<String,String>("Test","from@example.com");

    private EmailService service;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        Layout layout = new PatternLayout("%c{1} %x - %m%n");
        ConsoleAppender appender = new ConsoleAppender(layout);

        BasicConfigurator.configure(appender);

        Logger.getLogger(EmailService.class).addAppender(appender);

        Properties props = System.getProperties();
        Session session = Session.getInstance(props);

        service = new EmailService(session);
        service.setDummyMode(true);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        BasicConfigurator.resetConfiguration();
    }

    public void testTemplate()
    {
        service.registerTemplate(
                "test",
                new Template(
                    "Hi ${a},\n\nFrom,\n- ${b}",
                    EmailService.ContentType.PLAIN
                )
            );

        // Test the template-not-found case
        try
        {
            service.sendTemplate(
                    FROM, "to@example.com", "Test Message", "non-existent-template",
                    new KeyValuePair("a", "a"),
                    new KeyValuePair("b", "b")
                );
        }
        catch (TemplateException te)
        {
            Set<String> tokens = te.getUnreplacedTokens();
            assertEquals(tokens.size(), 0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unexpected exception!");
        }

        // Test the token-missing case
        try
        {
            service.sendTemplate(
                    FROM, "to@example.com", "Test Message", "test",
                    new KeyValuePair("a", "a")
                );
        }
        catch (TemplateException te)
        {
            Set<String> tokens = te.getUnreplacedTokens();
            assertEquals(tokens.size(), 1);
            assertEquals(tokens.iterator().next(), "${b}");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unexpected exception!");
        }

        // Test the success case
        try
        {
            service.sendTemplate(
                    FROM, "to@example.com", "Test Message", "test",
                    new KeyValuePair("a", "a"),
                    new KeyValuePair("b", "b")
                );
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Unexpected exception!");
        }
        
    }

    public void testSend() throws InterruptedException
    {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate   = new CountDownLatch(N_THREADS);

        for (int i = 0; i < N_THREADS; i++)
        {
            Thread thread = new Thread("Thread-" + i)
            {
                @Override
                public void run()
                {
                    try
                    {
                        startGate.await();

                        for (int i = 0; i < 100; i++)
                        {
                            service.sendPlain(
                                            FROM,
                                            "example@example.com",
                                            getName() + " #" + i,
                                            "Body",
                                            Priority.values()[((Double)(Math.floor(Priority.values().length * Math.random()))).intValue()]
                                        );
                        }

                    }
                    catch (Exception e)
                    {
                        out.println(getName() + " failed with an excepiton");
                        e.printStackTrace();
                    }
                    finally
                    {
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

        boolean normal = service.shutdown(30, TimeUnit.SECONDS);
        assertTrue(normal);
    }
}