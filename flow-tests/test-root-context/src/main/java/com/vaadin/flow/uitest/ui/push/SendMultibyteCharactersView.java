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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public abstract class SendMultibyteCharactersView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        CustomPush push = getClass().getAnnotation(CustomPush.class);

        attachEvent.getUI().getPushConfiguration().setPushMode(push.value());
        attachEvent.getUI().getPushConfiguration()
                .setTransport(push.transport());

        Div div = new Div();
        div.setText("Just a label");
        div.setId("label");
        add(div);
        Element area = ElementFactory.createTextarea();
        area.addPropertyChangeListener("value", "change", event -> {
        });
        area.setAttribute("id", "text");
        getElement().appendChild(area);
    }

}
