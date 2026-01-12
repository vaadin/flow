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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route(value = "com.vaadin.flow.uitest.ui.PreserveOnRefreshView")
@PreserveOnRefresh
public class PreserveOnRefreshView extends Div {

    final static String COMPONENT_ID = "contents";
    final static String NOTIFICATION_ID = "notification";
    final static String ATTACHCOUNTER_ID = "attachcounter";

    private int attached = 0;
    private final Div attachCounter;

    public PreserveOnRefreshView() {
        // create unique content for this instance
        final String uniqueId = Long.toString(new Random().nextInt());

        final Div componentId = new Div();
        componentId.setId(COMPONENT_ID);
        componentId.setText(uniqueId);
        add(componentId);

        // add an element to keep track of number of attach events
        attachCounter = new Div();
        attachCounter.setId(ATTACHCOUNTER_ID);
        attachCounter.setText("0");
        add(attachCounter);

        // also add an element as a separate UI child. This is expected to be
        // transferred on refresh (mimicking dialogs and notifications)
        final Element looseElement = new Element("div");
        looseElement.setProperty("id", NOTIFICATION_ID);
        looseElement.setText(uniqueId);
        UI.getCurrent().getElement().insertChild(0, looseElement);

        StreamResource resource = new StreamResource("filename",
                () -> new ByteArrayInputStream(
                        "foo".getBytes(StandardCharsets.UTF_8)));
        Anchor download = new Anchor("", "Download file");
        download.setHref(resource);
        download.setId("link");
        add(download);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        attached += 1;
        attachCounter.setText(Integer.toString(attached));
    }
}
