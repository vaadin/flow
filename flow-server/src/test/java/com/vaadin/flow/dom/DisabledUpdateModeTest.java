/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import static com.vaadin.flow.dom.DisabledUpdateMode.ALWAYS;
import static com.vaadin.flow.dom.DisabledUpdateMode.ONLY_WHEN_ENABLED;

import org.junit.Assert;
import org.junit.Test;

public class DisabledUpdateModeTest {

    @Test
    public void permissiveOrdering() {
        assertMostPermissive(ALWAYS, ALWAYS, ALWAYS);
        assertMostPermissive(ALWAYS, ALWAYS, ONLY_WHEN_ENABLED);
        assertMostPermissive(ALWAYS, ALWAYS, null);

        assertMostPermissive(ONLY_WHEN_ENABLED, ONLY_WHEN_ENABLED,
                ONLY_WHEN_ENABLED);
        assertMostPermissive(ONLY_WHEN_ENABLED, ONLY_WHEN_ENABLED, null);

        assertMostPermissive(null, null, null);
    }

    private static void assertMostPermissive(DisabledUpdateMode expectedResult,
            DisabledUpdateMode first, DisabledUpdateMode second) {

        Assert.assertEquals(expectedResult,
                DisabledUpdateMode.mostPermissive(first, second));
        Assert.assertEquals(expectedResult,
                DisabledUpdateMode.mostPermissive(second, first));
    }
}
