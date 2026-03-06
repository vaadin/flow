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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ScrollPositionLiveReloadView", layout = ViewTestLayout.class)
public class ScrollPositionLiveReloadView extends AbstractLiveReloadView {

    public ScrollPositionLiveReloadView() {
        // Button to trigger full-refresh (DOM patching, no full page reload)
        NativeButton refreshButton = new NativeButton(
                "Trigger UI Refresh (DOM patch)");
        refreshButton.addClickListener(e -> {
            BrowserLiveReloadAccessor liveReloadAccess = VaadinService
                    .getCurrent().getInstantiator()
                    .getOrCreate(BrowserLiveReloadAccessor.class);
            BrowserLiveReload browserLiveReload = liveReloadAccess
                    .getLiveReload(VaadinService.getCurrent());
            browserLiveReload.refresh(true);
        });
        refreshButton.setId("refresh-button");
        add(refreshButton);

        // Button to trigger full page reload
        NativeButton reloadButton = new NativeButton(
                "Trigger Full Page Reload");
        reloadButton.addClickListener(e -> {
            BrowserLiveReloadAccessor liveReloadAccess = VaadinService
                    .getCurrent().getInstantiator()
                    .getOrCreate(BrowserLiveReloadAccessor.class);
            BrowserLiveReload browserLiveReload = liveReloadAccess
                    .getLiveReload(VaadinService.getCurrent());
            browserLiveReload.reload();
        });
        reloadButton.setId("reload-button");
        add(reloadButton);

        // Outer scrollable container
        Div outerScroll = new Div();
        outerScroll.setId("outer-scroll");
        outerScroll.getStyle().set("height", "400px");
        outerScroll.getStyle().set("overflow", "auto");
        outerScroll.getStyle().set("border", "2px solid blue");

        // Inner scrollable container nested inside outer (no ID, to test
        // scroll restoration for elements identified by DOM path)
        Div innerScroll = new Div();
        innerScroll.getStyle().set("height", "200px");
        innerScroll.getStyle().set("overflow", "auto");
        innerScroll.getStyle().set("border", "2px solid red");
        innerScroll.getStyle().set("margin", "10px");

        // Items inside the inner scrollable container
        for (int i = 0; i < 50; i++) {
            Div item = new Div();
            item.setText("Inner item " + i);
            item.setId("inner-item-" + i);
            item.getStyle().set("padding", "8px");
            item.getStyle().set("border-bottom", "1px solid #eee");
            innerScroll.add(item);
        }

        outerScroll.add(innerScroll);

        // More items in the outer scrollable container (after the inner one)
        for (int i = 0; i < 50; i++) {
            Div item = new Div();
            item.setText("Outer item " + i);
            item.setId("outer-item-" + i);
            item.getStyle().set("padding", "8px");
            item.getStyle().set("border-bottom", "1px solid #ddd");
            outerScroll.add(item);
        }

        add(outerScroll);

        // Items below the scrollable containers for window-level scroll
        for (int i = 0; i < 100; i++) {
            Div item = new Div();
            item.setText("Item " + i);
            item.setId("item-" + i);
            item.getStyle().set("padding", "10px");
            item.getStyle().set("border-bottom", "1px solid #ccc");
            add(item);
        }

        Span bottomMarker = new Span("Bottom of page");
        bottomMarker.setId("bottom-marker");
        add(bottomMarker);
    }
}
