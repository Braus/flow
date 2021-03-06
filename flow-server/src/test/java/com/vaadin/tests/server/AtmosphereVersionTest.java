package com.vaadin.tests.server;

import org.atmosphere.util.Version;

import com.vaadin.flow.server.Constants;

import junit.framework.TestCase;

public class AtmosphereVersionTest extends TestCase {
    /**
     * Test that the atmosphere version constant matches the version on our
     * classpath
     */
    public void testAtmosphereVersion() {
        assertEquals(Constants.REQUIRED_ATMOSPHERE_RUNTIME_VERSION,
                Version.getRawVersion());
    }
}
