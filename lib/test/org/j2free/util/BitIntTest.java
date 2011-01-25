package org.j2free.util;

import junit.framework.TestCase;

/**
 *
 * @author ryan
 */
public class BitIntTest extends TestCase {
    
    public BitIntTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void printIntValue(String prefix, BitInt instance)
    {
        System.out.println(prefix + ": instance.intValue() = " + Integer.toString(instance.intValue(),2));
    }

    /**
     * Test of isSet method, of class BitInt.
     */
    public void testIsSet()
    {
        int pos = 0x1 << 1;
        System.out.println("\nisSet test with pos = " + Integer.toString(pos,2));

        // OnFlag.ZERO test
        BitInt instance = new BitInt(0, BitInt.OnFlag.ZERO);
        printIntValue("init ZERO test", instance);
        assertEquals(true, instance.isSet(pos));

        instance.set(pos, 1);
        printIntValue("set pos to 1", instance);
        assertEquals(false, instance.isSet(pos));

        // OnFlag.ONE test
        instance = new BitInt(0, BitInt.OnFlag.ONE);
        printIntValue("init ONE test", instance);
        assertEquals(false, instance.isSet(pos));

        instance.set(pos, 1);
        printIntValue("set pos to 1", instance);
        assertEquals(true, instance.isSet(pos));
    }

    /**
     * Test of set method, of class BitInt.
     */
    public void testSet_int_boolean()
    {
        int pos = 0x1 << 1;
        System.out.println("\nset(int,boolean) test with pos = " + Integer.toString(pos,2));

        // OnFlag.ONE test
        BitInt instance = new BitInt(0, BitInt.OnFlag.ONE);
        printIntValue("init ONE test", instance);

        instance.set(pos, true);
        printIntValue("set pos to on", instance);
        assertEquals(true, instance.isSet(pos));
        assertEquals(false, instance.isSet(pos >> 1));
        assertEquals(false, instance.isSet(pos << 1));

        instance.set(pos, false);
        printIntValue("set pos to off", instance);
        assertEquals(false, instance.isSet(pos));
        assertEquals(false, instance.isSet(pos >> 1));
        assertEquals(false, instance.isSet(pos << 1));

        // OnFlag.ZERO test
        instance = new BitInt(0, BitInt.OnFlag.ZERO);
        printIntValue("init ZERO test", instance);

        assertEquals(true, instance.isSet(pos));
        assertEquals(true, instance.isSet(pos >> 1));
        assertEquals(true, instance.isSet(pos << 1));

        instance.set(pos, false);
        printIntValue("set pos to off", instance);
        assertEquals(false, instance.isSet(pos));
        assertEquals(true, instance.isSet(pos >> 1));
        assertEquals(true, instance.isSet(pos << 1));
    }

    /**
     * Test of set method, of class BitInt.
     */
    public void testSet_int_int()
    {
        int pos = 0x1 << 1;
        System.out.println("\nset(int,int) test with pos = " + Integer.toString(pos,2));

        // OnFlag.ONE test
        BitInt instance = new BitInt(0, BitInt.OnFlag.ONE);
        printIntValue("init ONE test", instance);
        
        instance.set(pos, 1);
        printIntValue("set pos to 1", instance);
        assertEquals(true, instance.isSet(pos));
        assertEquals(false, instance.isSet(pos >> 1));
        assertEquals(false, instance.isSet(pos << 1));

        instance.set(pos, 0);
        printIntValue("set pos to 0", instance);
        assertEquals(false, instance.isSet(pos));
        assertEquals(false, instance.isSet(pos >> 1));
        assertEquals(false, instance.isSet(pos << 1));

        // OnFlag.ZERO test
        instance = new BitInt(0, BitInt.OnFlag.ZERO);
        printIntValue("init ZERO test", instance);

        assertEquals(true, instance.isSet(pos));
        assertEquals(true, instance.isSet(pos >> 1));
        assertEquals(true, instance.isSet(pos << 1));

        instance.set(pos, 1);
        printIntValue("set pos to 0", instance);
        assertEquals(false, instance.isSet(pos));
        assertEquals(true, instance.isSet(pos >> 1));
        assertEquals(true, instance.isSet(pos << 1));

        instance.set(pos, 0);
        printIntValue("set pos to 1", instance);
        assertEquals(true, instance.isSet(pos));
        assertEquals(true, instance.isSet(pos >> 1));
        assertEquals(true, instance.isSet(pos << 1));
    }

    /**
     * Test of toggle method, of class BitInt.
     */
    public void testToggle()
    {
        int pos = 0x1 << 1;
        System.out.println("\ntoggle test with pos = " + Integer.toString(pos, 2));

        // OnFlag.ONE test
        BitInt instance = new BitInt(0, BitInt.OnFlag.ONE);
        printIntValue("init ONE test", instance);
        assertEquals(false, instance.isSet(pos));

        instance.toggle(pos);
        printIntValue("toggle pos", instance);
        assertEquals(true, instance.isSet(pos));

        instance.toggle(pos);
        printIntValue("toggle pos", instance);
        assertEquals(false, instance.isSet(pos));

        // OnFlag.ZERO test
        instance = new BitInt(0, BitInt.OnFlag.ZERO);
        printIntValue("init ZERO test", instance);
        assertEquals(true, instance.isSet(pos));

        instance.toggle(pos);
        printIntValue("toggle pos", instance);
        assertEquals(false, instance.isSet(pos));

        instance.toggle(pos);
        printIntValue("toggle pos", instance);
        assertEquals(true, instance.isSet(pos));
    }
}
