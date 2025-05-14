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
package com.vaadin.flow.uitest.ui.routing;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.osgi.OSGiMarker;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("com.vaadin.flow.uitest.ui.PushLayout")
public class PushLayout extends Div
        implements RouterLayout, BeforeEnterObserver {

    public static String FORWARD_PATH = "forward-no-route";

    public PushLayout() {
        setId("push-layout");
        Lookup lookup = VaadinService.getCurrent().getContext()
                .getAttribute(Lookup.class);
        if (lookup.lookup(OSGiMarker.class) == null) {
            PushConfiguration pushConfiguration = UI.getCurrent()
                    .getPushConfiguration();
            pushConfiguration.setPushMode(PushMode.AUTOMATIC);
            pushConfiguration.setTransport(Transport.WEBSOCKET_XHR);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (FORWARD_PATH.equals(event.getLocation().getPath())) {
            event.forwardTo(ForwardPage.class);
        }
    }

}
