package org.j2free.jsp.el;

import junit.framework.TestCase;

/**
 *
 * @author ryan
 */
public class StandardExtensionsTest extends TestCase
{
    public StandardExtensionsTest()
    {
        super("StandardExtensionsTest");
    }

    public void testIpToInt()
    {
        String ipStr = "98.234.145.140";
        int ip = StandardExtensions.ipToInt(ipStr);
        assertEquals("Expected 1659539852 but got " + ip, ip, 1659539852);
    }

    public void testIntToIp()
    {
        int ip = 1659539852;
        String ipStr = StandardExtensions.intToIp(ip);
        assertEquals("Expected 98.234.145.140 but got " + ipStr, ipStr, "98.234.145.140");
    }
}
