/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("performance/constant-pool")
public class ConstantPoolPerformanceView extends AbstractDivView {

    Div container = new Div();
    Div notification = new Div();
    public ConstantPoolPerformanceView() {
        NativeButton btn = new NativeButton("refresh");
        btn.addClickListener(e->{
            container.removeAll();
            for (int i = 0; i <1000; i++) {
                container.add(new H1("Headline " + i));
                container.add(new NativeButton("BTN " + i, e2 -> notification.setText("clicked")));
            }
            UI.getCurrent().getPage().executeJs("setTimeout(function(){$0.click()},2000)", btn);
        });
        add(btn,notification,container);
    }

}
