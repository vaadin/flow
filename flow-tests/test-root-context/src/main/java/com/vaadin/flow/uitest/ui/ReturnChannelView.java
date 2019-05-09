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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ReturnChannelView", layout = ViewTestLayout.class)
public class ReturnChannelView extends AbstractDivView {
    public ReturnChannelView() {
        Element button = new Element("button");
        button.setAttribute("id", "button");
        button.setText("Send message to channel");

        ReturnChannelRegistration channel = button.getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> button.setText(
                        "Click registered: " + arguments.getString(0)));

        button.executeJs(
                "this.addEventListener('click', function() { $0('hello') })",
                channel);

        getElement().appendChild(button);
    }
}
