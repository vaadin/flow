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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.JavaLiveReloadView", layout = ViewTestLayout.class)
public class JavaLiveReloadView extends AbstractLiveReloadView {
    public static final String JAVA_LIVE_RELOAD_TRIGGER_BUTTON = "java-live-reload-trigger-button";

    public JavaLiveReloadView() {
        NativeButton javaReloadButton = new NativeButton(
                "Trigger Java live reload");
        javaReloadButton.addClickListener(this::handleClickJavaLiveReload);
        javaReloadButton.setId(JAVA_LIVE_RELOAD_TRIGGER_BUTTON);
        add(javaReloadButton);
    }

    // Java triggered live reload is faked as we do not have Trava JDK in test
    private void handleClickJavaLiveReload(ClickEvent<?> event) {
        BrowserLiveReloadAccessor liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccessor.class);
        BrowserLiveReload browserLiveReload = liveReloadAccess
                .getLiveReload(VaadinService.getCurrent());
        browserLiveReload.reload();
    }
}
