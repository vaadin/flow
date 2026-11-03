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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.uitest.ui.AbstractDivView;

/**
 * Pushes in the middle of a request, from the attach listener of a component
 * added in a click listener, see https://github.com/vaadin/flow/issues/16635
 */
@Route("com.vaadin.flow.uitest.ui.push.PushInAttachListenerSsePureView")
@Push(value = PushMode.MANUAL, transport = Transport.SSE_PURE)
public class PushInAttachListenerSsePureView extends AbstractDivView {
    static final String ADD_BUTTON = "add";
    static final String COUNT_BUTTON = "count";
    static final String ATTACHED_CLASS = "attached";
    static final String COUNT_ID = "count-value";

    private int count;

    public PushInAttachListenerSsePureView() {
        Span countValue = new Span("0");
        countValue.setId(COUNT_ID);
        add(countValue);
        add(createButton("Add a component that pushes on attach", ADD_BUTTON,
                e -> {
                    Div child = new Div(new Span("attached"));
                    child.addClassName(ATTACHED_CLASS);
                    child.addAttachListener(attach -> attach.getUI().push());
                    add(child);
                }));
        add(createButton("Count", COUNT_BUTTON,
                e -> countValue.setText(String.valueOf(++count))));
    }
}
