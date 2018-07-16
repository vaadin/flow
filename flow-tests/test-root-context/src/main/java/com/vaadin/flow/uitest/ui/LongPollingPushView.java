/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Class for reproducing the bug https://github.com/vaadin/flow/issues/4353
 */
@Route("com.vaadin.flow.uitest.ui.LongPollingPushView")
@Push(transport = Transport.LONG_POLLING)
public class LongPollingPushView extends AbstractDivView {

    public LongPollingPushView() {
        Div parent = new Div();
        Span child = new Span("Some text");
        child.setId("child");
        parent.add(child);
        add(parent);
        parent.setVisible(false);

        NativeButton visibility = new NativeButton("Toggle visibility",
                e -> parent.setVisible(!parent.isVisible()));
        visibility.setId("visibility");
        add(visibility);
    }
}
