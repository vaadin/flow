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
package com.vaadin.flow.tutorial.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcons;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("flow-components/tutorial-flow-icon.asciidoc")
public class IconBasic {

    public void basics() {
        Icon icon = VaadinIcons.VAADIN_H.create();
        new Button("Vaadin", icon);
    }

    public void customCollection() {
        Icon icon = new Icon("valo", "clock");
        new Button("Clock", icon);
    }
}
