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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.RemoveElementAfterItsChildrenView")
public class RemoveElementAfterItsChildrenView extends Div {

    public RemoveElementAfterItsChildrenView() {
        Div container = new Div();
        container.setId("container");
        Div wrapper = new Div();
        wrapper.setId("wrapper");
        Div first = new Div();
        first.setText("First child");
        first.setId("first");
        Div second = new Div();
        second.setText("Second child");
        second.setId("second");
        container.add(wrapper);
        wrapper.add(first, second);

        NativeButton button = new NativeButton();
        button.setText("Move children and remove parent");
        button.setId("move-children");
        add(container, button);

        // Then later on click the button, executing the following code
        button.addClickListener(event -> {
            container.add(first, second);
            wrapper.getElement().removeFromParent();
        });
    }

}
