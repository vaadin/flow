/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

import org.junit.Assert;
import org.junit.Test;

public class AccessCheckResultTest {

    @Test
    public void create_getsResultInstance() {
        for (AccessCheckDecision decision : AccessCheckDecision.values()) {
            AccessCheckResult result = AccessCheckResult.create(decision,
                    decision.name());
            Assert.assertEquals(decision, result.decision());
            Assert.assertEquals(decision.name(), result.reason());
        }
    }

    @Test
    public void create_nullReason_throws() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> AccessCheckResult.create(null, "Something"));
    }

    @Test
    public void create_denyWithoutReason_throws() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> AccessCheckResult.create(AccessCheckDecision.DENY, null));
    }

    @Test
    public void create_rejectWithoutReason_throws() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> AccessCheckResult.create(AccessCheckDecision.REJECT,
                        null));
    }

    @Test
    public void deny_noReason_throws() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> AccessCheckResult.deny(null));
    }

    @Test
    public void reject_noReason_throws() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> AccessCheckResult.reject(null));
    }

}