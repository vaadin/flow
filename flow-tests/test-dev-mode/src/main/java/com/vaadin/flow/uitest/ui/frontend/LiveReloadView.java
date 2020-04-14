/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.uitest.ui.frontend;

import java.util.Random;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.frontend.LiveReloadView", layout = ViewTestLayout.class)
public class LiveReloadView extends Div {

    Integer instanceIdentifier = new Random().nextInt();

    public LiveReloadView() {
        Label label = new Label(Integer.toString(instanceIdentifier));
        label.setId("elementId");
        NativeButton reloadButton = new NativeButton("Trigger live reload");
        reloadButton.addClickListener(this::handleClickLiveReload);
        reloadButton.setId("live-reload-trigger-button");
        add(label);
        add(reloadButton);
    }

    private void handleClickLiveReload(ClickEvent event) {
        BrowserLiveReloadAccess liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccess.class);
        BrowserLiveReload browserLiveReload = liveReloadAccess
                .getLiveReload(VaadinService.getCurrent());
        browserLiveReload.reload();
    }
}
