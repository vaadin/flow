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

package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.BackNavSecondView")
public class BackNavSecondView extends Div implements AfterNavigationObserver {

    public static final String CALLS = "calls";
    private int count = 0;
    Span text = new Span("Second view: " + count);

    public BackNavSecondView() {
        text.setId(CALLS);
        add(text);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        count++;
        text.setText("Second view: " + count);
        UI.getCurrent().getPage().getHistory().replaceState(null,
                "com.vaadin.flow.BackNavSecondView?param");
    }
}
