/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.annotations.ClientDelegate;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.model.TemplateModel;

public class JSWithComponentsView extends Div implements View {

    public static class JSDiv extends Div {
        public JSDiv() {
            setText("initial");
            setId("div");
        }

        @ClientDelegate
        public void method1() {
            setText(getText() + "-method1");
        }

        @ClientDelegate
        public void method2(int value) {
            setText(getText() + "-method2[" + value + "]");
        }
    }

    public JSWithComponentsView() {
        add(new JSDiv());
        add(new InlineTemplate<>(
                "<div><button id=button1 (click)='div.$server.method1();'>Trigger method1</button>"
                        + "<button id=button2 (click)='div.$server.method2(12);'>Trigger method2(12)</button></div>",
                TemplateModel.class));
    }
}
