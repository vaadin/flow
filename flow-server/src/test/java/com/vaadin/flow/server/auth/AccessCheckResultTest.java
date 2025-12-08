/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
