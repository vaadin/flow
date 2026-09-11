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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenInNewTabActionTest {

    private static final String EXPECTED_BODY = "((u) =>"
            + " /^[\\x00-\\x20]*javascript:/i.test(String(u))"
            + " || window.open(u, \"_blank\", $1(event)))($0(event))";

    @Test
    void urlString_emitsWindowOpenWithBlankTargetAndDefaultFeatures() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new OpenInNewTabAction("https://vaadin.com/docs"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // The action body wraps window.open in an IIFE that short-circuits
        // when the resolved URL starts with javascript:. URL and features
        // each live on their own input JsFunction captured by the action.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(EXPECTED_BODY, action.getBody());

        JsFunction url = (JsFunction) action.getCaptures().get(0);
        assertEquals("https://vaadin.com/docs", url.getCaptures().get(0));

        JsFunction features = (JsFunction) action.getCaptures().get(1);
        assertEquals("noopener,noreferrer", features.getCaptures().get(0));
    }

    @Test
    void urlStringWithFeatures_passesCustomFeaturesVerbatim() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new OpenInNewTabAction(
                "/help", "noopener,width=600,height=400"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Supplying features replaces — does not extend — the defaults.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(EXPECTED_BODY, action.getBody());

        JsFunction features = (JsFunction) action.getCaptures().get(1);
        assertEquals("noopener,width=600,height=400",
                features.getCaptures().get(0));
    }

    @Test
    void inputUrl_splicesPropertyInputThatReadsAtFireTime() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click").triggers(new OpenInNewTabAction(
                new PropertyInput<>(field, "value", String.class)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // The URL slot now holds a PropertyInput JsFunction that reads
        // field["value"] on the client at fire time.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(EXPECTED_BODY, action.getBody());

        JsFunction url = (JsFunction) action.getCaptures().get(0);
        assertEquals("return $0[$1]", url.getBody());
        assertEquals(field.getElement(), url.getCaptures().get(0));
        assertEquals("value", url.getCaptures().get(1));
    }

    @Test
    void javascriptUrl_rejectedByConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("javascript:alert(1)"));
    }

    @Test
    void javascriptUrlCaseAndWhitespaceVariants_allRejected() {
        // Mixed case: scheme is case-insensitive per the URL spec.
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("JavaScript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("JAVASCRIPT:alert(1)"));
        // Leading whitespace and C0 controls are stripped by the URL parser
        // before scheme matching, so they don't let the URL through.
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("  javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("\tjavascript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("\njavascript:alert(1)"));
        // Two-arg constructor also rejects.
        assertThrows(IllegalArgumentException.class,
                () -> new OpenInNewTabAction("javascript:alert(1)",
                        "noopener,noreferrer"));
    }
}
