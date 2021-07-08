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

import java.util.UUID;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.NonExitingImageView")
public class NonExitingImageView extends Div {

    public static final String IMAGE_NON_EXISTENT = "not_found.jpeg";

    public static final String ID = UUID.randomUUID().toString();

    public NonExitingImageView() {
        Element img = new Element(Tag.IMG);
        img.setAttribute("src", IMAGE_NON_EXISTENT);
        img.setAttribute("alt", "alt");
        getElement().appendChild(img);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attachEvent.getUI().getPushConfiguration()
                .setPushMode(PushMode.AUTOMATIC);
        attachEvent.getUI().getPushConfiguration()
                .setTransport(Transport.LONG_POLLING);
        ComponentUtil.setData(attachEvent.getUI(), ID, ID);
    }
}
