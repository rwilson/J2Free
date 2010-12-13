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
        MixpanelClient client = new MixpanelClient("dc864fba5af121e62ef6106d83d21f19");
        client.setDebug(true);

        // start a http service
        SimpleHttpService.init(5, -1, 600, 30, 30);
        assertTrue(SimpleHttpService.isEnabled());

        // Test a track event
        client.track("test-event", null, null, new KeyValuePair("test-id", "a"));

        // Test a track event and check the response
        Future<HttpCallResult> future = client.track("test-event", null, null, new KeyValuePair("test-id", "b"));
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
