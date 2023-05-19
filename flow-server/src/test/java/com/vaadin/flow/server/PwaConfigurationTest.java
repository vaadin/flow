package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Test;

public class PwaConfigurationTest {
    @Test
    // For https://github.com/vaadin/flow/issues/10148
    public void pwaDefaultStartUrl_should_BeDotInsteadOfEmptyString() {
        PwaConfiguration pwaConfiguration = new PwaConfiguration();
        Assert.assertEquals(PwaConfiguration.DEFAULT_START_URL,
                pwaConfiguration.getStartUrl());
    }

    @PWA(name = "name", shortName = "shortName")
    static class App {
    }

    @Test
    public void pwaOfflinePathEmpty_should_beDisabled() {
        PwaConfiguration pwaConfiguration = new PwaConfiguration(
                App.class.getAnnotation(PWA.class));
        Assert.assertFalse(pwaConfiguration.isOfflinePathEnabled());
        Assert.assertEquals("", pwaConfiguration.getOfflinePath());
    }
}
