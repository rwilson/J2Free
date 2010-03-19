
package org.j2free.error;

import java.util.concurrent.Future;
import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.j2free.http.HttpCallResult;
import org.j2free.http.SimpleHttpService;

/**
 * @author Ryan
 */
public class HoptoadErrorReporterTest extends TestCase
{
    private HoptoadErrorReporter instance;

    public HoptoadErrorReporterTest(String testName)
    {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Layout layout = new PatternLayout("%c{1} %x - %m%n");
        ConsoleAppender appender = new ConsoleAppender(layout);
        BasicConfigurator.configure(appender);

        instance = new HoptoadErrorReporter("c21f53900886af66b3d1de734bec7728", "2.0");

        SimpleHttpService.init(1, 1, 60, 30, 30);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        BasicConfigurator.resetConfiguration();
        SimpleHttpService.shutdownNow();
    }

    /**
     * Test of notify method, of class HoptoadErrorReporter.
     */
    public void testNotify_Throwable()
    {
        BuggyProgram p = new BuggyProgram();
        try
        {
            p.execute();
        }
        catch (Exception e)
        {
            System.out.println("notify_Throwable");
            Future<HttpCallResult> future = instance.notify(e);
            try
            {
                HttpCallResult result = future.get();
                assertEquals(result.getStatusCode(), 200);
            }
            catch (Exception f)
            {
                f.printStackTrace();
                fail();
            }
        }
    }

    /**
     * Test of notify method, of class HoptoadErrorReporter.
     */
    public void testNotify_Throwable_String()
    {
        BuggyProgram p = new BuggyProgram();
        try
        {
            p.execute();
        }
        catch (Exception e)
        {
            System.out.println("notify_Throwable_String");
            Future<HttpCallResult> future = instance.notify(e, "TEST: Hit a bug");
            try
            {
                HttpCallResult result = future.get();
                assertEquals(result.getStatusCode(), 200);
            }
            catch (Exception f)
            {
                f.printStackTrace();
                fail();
            }
        }
    }

    private class BuggyProgram
    {
        public void execute()
        {
            throw new RuntimeException("TEST: A bug!");
        }
    }
}
