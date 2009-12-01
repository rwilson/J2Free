package org.j2free.mixpanel;

import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.j2free.http.HttpCallFuture;
import org.j2free.http.HttpCallResult;
import org.j2free.http.QueuedHttpCallService;
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

        try {
            Class.forName("org.j2free.http.QueuedHttpCallService");
        } catch (ClassNotFoundException e) {
            fail("QueuedHttpCallService could not be called!");
        }

        MixpanelAPIClient client = MixpanelAPIClient.get();
        assertNull(client);

        MixpanelAPIClient.init("dc864fba5af121e62ef6106d83d21f19");
        client = MixpanelAPIClient.get();
        assertNotNull(client);

        QueuedHttpCallService.enable(-1, 5, 30, 30);
        assertTrue(QueuedHttpCallService.isEnabled());

        client.enableDebug();
        client.trackEvent("test-event", null, null, new KeyValuePair("test-id", "a"));

        HttpCallFuture future = MixpanelAPIClient.track("test-event", null, null, new KeyValuePair("test-id", "b"));
        try {
            HttpCallResult result = future.get();
            System.out.println("result [status=" + result.getStatusCode() + ", body=" + result.getResponse() + "]");
            assertEquals(result.getResponse(), "1");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        try {
            boolean normal = QueuedHttpCallService.shutdown(30, TimeUnit.SECONDS);
            assertTrue(normal);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
