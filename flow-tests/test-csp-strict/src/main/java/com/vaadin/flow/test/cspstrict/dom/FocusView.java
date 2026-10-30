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
package com.vaadin.flow.test.cspstrict.dom;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;

/**
 * Framework JavaScript that already runs from the bundle: focusing an element
 * and setting the page title.
 */
@Route("focus")
public class FocusView extends Div {

    static final String TITLE = "Focused under the policy";

    private final Input input = new Input();

    public FocusView() {
        input.setId("input");
        add(input);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        input.focus();
        attachEvent.getUI().getPage().setTitle(TITLE);
    }
}
