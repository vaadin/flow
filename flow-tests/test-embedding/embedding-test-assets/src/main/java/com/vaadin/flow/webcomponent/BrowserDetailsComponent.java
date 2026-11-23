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
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.signals.Signal;

public class BrowserDetailsComponent extends Div {

    public BrowserDetailsComponent() {
        Div screenWidth = new Div();
        screenWidth.setId("screen-width");

        Div webShareSupport = new Div();
        webShareSupport.setId("web-share-support");

        NativeButton refresh = new NativeButton("Refresh",
                event -> UI.getCurrent().getPage().getExtendedClientDetails()
                        .refresh(details -> screenWidth.setText(
                                String.valueOf(details.getScreenWidth()))));
        refresh.setId("refresh");

        Signal<WebShareSupport> supportSignal = WebShare
                .supportSignal(UI.getCurrent());
        Signal.effect(this,
                () -> webShareSupport.setText(supportSignal.get().name()));

        add(refresh, screenWidth, webShareSupport);
    }
}
