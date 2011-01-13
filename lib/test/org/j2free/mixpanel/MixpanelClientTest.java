package org.j2free.mixpanel;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;

import org.j2free.http.HttpCallResult;
import org.j2free.http.SimpleHttpService;

/**
 * @author Ryan Wilson
 */
public class MixpanelClientTest extends TestCase {

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
    
    public void testInit()
    {
        // Create a client in debug mode
        MixpanelClient mpmetrics = new MixpanelClient("98f0199604620f3fedf96d71929a58d9");
        mpmetrics.setTest(true);

        // start a http service
        SimpleHttpService.init(5, -1, 600, 30, 30);
        assertTrue(SimpleHttpService.isEnabled());

        mpmetrics.registerProperty("1", "loggedIn", Boolean.TRUE.toString());

        // Test a track event and check the response
        Future<HttpCallResult> future = mpmetrics.track("Account.changeUsername", "1", "98.234.145.179");
        try {
            HttpCallResult result = future.get();
            assertEquals(result.getResponse(), "1");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // shutdown the http service
        try {
            boolean normal = SimpleHttpService.shutdown(30, TimeUnit.SECONDS);
            assertTrue(normal);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
