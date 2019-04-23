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

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.CallFunctionBeforeRemoveView", layout = ViewTestLayout.class)
public class CallFunctionBeforeRemoveView extends AbstractDivView {

    public CallFunctionBeforeRemoveView(){
        Input input = new Input();

        add(input);

        NativeButton button = new NativeButton("Call function and detach");
        add(button);
        button.addClickListener(event -> {
            if (input.getParent().isPresent()) {
                input.getElement().callJsFunction("focus");
                remove(input);
            }
        });
    }
}
