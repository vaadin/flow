/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.signals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectContextTest {

    @Test
    void isInitialRun_returnsValueFromConstructor() {
        EffectContext initial = new EffectContext(true, false);
        assertTrue(initial.isInitialRun());

        EffectContext subsequent = new EffectContext(false, false);
        assertFalse(subsequent.isInitialRun());
    }

    @Test
    void isBackgroundChange_falseOnInitialRun() {
        EffectContext ctx = new EffectContext(true, true);
        assertFalse(ctx.isBackgroundChange(),
                "Initial run should never be a background change even if invalidated from background");
    }

    @Test
    void isBackgroundChange_trueWhenNotInitialAndInvalidatedFromBackground() {
        EffectContext ctx = new EffectContext(false, true);
        assertTrue(ctx.isBackgroundChange());
    }

    @Test
    void isBackgroundChange_falseWhenNotInitialAndInvalidatedFromRequest() {
        EffectContext ctx = new EffectContext(false, false);
        assertFalse(ctx.isBackgroundChange());
    }
}
