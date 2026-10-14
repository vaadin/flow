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
package com.vaadin.flow.component.trigger.internal;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeoutTriggerTest {

    @Test
    void install_armsSetTimeoutWithDelay() {
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        ui.getElement().appendChild(host.getElement());

        new TimeoutTrigger(host, Duration.ofMillis(250))
                .triggers(new SetPropertyAction<>(host, "value", ""));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Action at $0, delay in milliseconds at $1; the returned function
        // clears the timer on removal. No user content leaks into the body.
        JsFunction install = singleInstallFn(ui);
        assertEquals("const id = setTimeout(() => $0(), $1);"
                + "return () => clearTimeout(id);", install.getBody());
        assertEquals(250L, install.getCaptures().get(1));
    }

    @Test
    void negativeDelay_throws() {
        UI ui = new MockUI();
        TagComponent host = new TagComponent("div");
        ui.getElement().appendChild(host.getElement());

        assertThrows(IllegalArgumentException.class,
                () -> new TimeoutTrigger(host, Duration.ofMillis(-1)));
    }
}
