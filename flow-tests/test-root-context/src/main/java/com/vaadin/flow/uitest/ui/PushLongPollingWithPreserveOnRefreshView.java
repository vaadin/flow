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

package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@PreserveOnRefresh
@Push(transport = Transport.LONG_POLLING)
@Route("com.vaadin.flow.uitest.ui.PushLongPollingWithPreserveOnRefreshView")
public class PushLongPollingWithPreserveOnRefreshView extends Div {

    public static final String ADD_BUTTON_ID = "add-button-id";
    public static final String TEST_DIV_ID = "test-div-id";
    public static final String TEXT_IN_DIV = "text in div";

    public PushLongPollingWithPreserveOnRefreshView() {
        NativeButton button = new NativeButton("Open Dialog",
                e -> e.getSource().getUI().ifPresent(ui -> {
                    Div div = new Div();
                    div.setText(TEXT_IN_DIV);
                    div.setId(TEST_DIV_ID);
                    ui.add(div);
                }));
        button.setId(ADD_BUTTON_ID);
        add(button);
    }
}
