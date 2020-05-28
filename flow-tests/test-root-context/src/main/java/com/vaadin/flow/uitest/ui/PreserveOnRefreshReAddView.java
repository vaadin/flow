/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.PreserveOnRefreshReAddView")
@PreserveOnRefresh
public class PreserveOnRefreshReAddView extends Div {

    public PreserveOnRefreshReAddView() {
        Text text1 = new Text("Text");
        Text text2 = new Text("Another Text");

        Div container = new Div();
        container.setId("container");

        NativeButton setText = new NativeButton("Set text", e -> {
            container.removeAll();
            container.add(text1);
        });
        NativeButton setAnotherText = new NativeButton("Set another text",
                e -> {
                    container.removeAll();
                    container.add(text2);
                });

        setText.setId("set-text");
        setAnotherText.setId("set-another-text");

        add(setText, setAnotherText, container);
    }
}
