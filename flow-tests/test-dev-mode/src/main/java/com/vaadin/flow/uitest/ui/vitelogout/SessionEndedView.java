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
package com.vaadin.flow.uitest.ui.vitelogout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/**
 * View displayed after successful logout.
 * <p>
 * Used to verify that the browser correctly navigated to this page after
 * session invalidation, instead of being redirected by Vite's page reload.
 */
@Route("com.vaadin.flow.uitest.ui.vitelogout.SessionEndedView")
public class SessionEndedView extends Div {

    public SessionEndedView() {
        Span marker = new Span("Session Ended Successfully");
        marker.setId("session-ended-marker");
        add(marker);
    }
}
