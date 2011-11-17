/*
 * Copyright 2011 Midokura KK
 */

package com.midokura.midolman.layer4;

import org.junit.After;
import org.junit.Before;

import com.midokura.midolman.util.Cache;
import com.midokura.midolman.voldemort.VoldemortTester;

public class TestNatLeaseManagerWithVoldemort extends TestNatLeaseManager {

    private VoldemortTester voldemort;

    @Override
    protected Cache createCache() {
        return voldemort.constructCache();
    }

    @Before
    public void setUp() throws Exception {
        voldemort = new VoldemortTester("midonat", 60000L, 4);
        voldemort.setUp();
        super.setUp();
    }

    @After
    public void tearDown() {
        voldemort.tearDown();
        voldemort = null;
    }
}
