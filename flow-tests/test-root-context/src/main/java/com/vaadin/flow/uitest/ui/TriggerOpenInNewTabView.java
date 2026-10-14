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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.OpenInNewTabAction;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Three buttons exercising {@link OpenInNewTabAction}: a static URL with
 * default features, a static URL with a custom features string (which replaces
 * the defaults verbatim), and an {@link Input}-backed {@link PropertyInput}
 * resolved at fire time. The IT replaces {@code window.open} with a recording
 * shim so the assertions don't depend on the browser actually opening a popup —
 * and so the popup blocker doesn't interfere.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerOpenInNewTabView", layout = ViewTestLayout.class)
public class TriggerOpenInNewTabView extends AbstractDivView {

    /**
     * Custom features used by the {@code #open-tab-features} button. Includes
     * {@code noopener,noreferrer} explicitly so the assertion still proves
     * round-trip without surrendering the security defaults.
     */
    static final String CUSTOM_FEATURES = "noopener,noreferrer,width=600,height=400";

    @Override
    protected void onShow() {
        NativeButton urlButton = new NativeButton("Open URL");
        urlButton.setId("open-tab");
        NativeButton urlWithFeaturesButton = new NativeButton(
                "Open URL with custom features");
        urlWithFeaturesButton.setId("open-tab-features");
        NativeButton inputButton = new NativeButton("Open URL from input");
        inputButton.setId("open-tab-input");
        Input urlField = new Input();
        urlField.setId("url-source");
        // Wired with an Input-based URL so a malicious value typed into the
        // field exercises the client-side javascript: guard (the constructor
        // already blocks static javascript: strings server-side).
        NativeButton jsInputButton = new NativeButton(
                "Open javascript: URL from input");
        jsInputButton.setId("open-tab-js-input");

        add(urlButton, urlWithFeaturesButton, inputButton, urlField,
                jsInputButton);

        new ClickTrigger(urlButton)
                .triggers(new OpenInNewTabAction("https://example.com/docs"));
        new ClickTrigger(urlWithFeaturesButton).triggers(new OpenInNewTabAction(
                "https://example.com/help", CUSTOM_FEATURES));
        new ClickTrigger(inputButton).triggers(new OpenInNewTabAction(
                new PropertyInput<>(urlField, "value", String.class)));
        new ClickTrigger(jsInputButton).triggers(new OpenInNewTabAction(
                new PropertyInput<>(urlField, "value", String.class)));
    }
}
