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
import org.j2free.util.KeyValuePair;

/**
 * @author Ryan Wilson
 */
public class MixpanelAPIClientTest extends TestCase {

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
    
    public void testInit() {

        // Create a client in debug mode
        MixpanelClient mpmetrics = new MixpanelClient("98f0199604620f3fedf96d71929a58d9");
//        mpmetrics.setDebug(true);

        // start a http service
        SimpleHttpService.init(5, -1, 600, 30, 30);
        assertTrue(SimpleHttpService.isEnabled());

        mpmetrics.registerProperty("user1", "loggedIn", Boolean.FALSE.toString());

        // Test a track event and check the response
        Future<HttpCallResult> future = mpmetrics.track("test-event", "user1", "98.234.145.140", new KeyValuePair("test-id", "a"));
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
