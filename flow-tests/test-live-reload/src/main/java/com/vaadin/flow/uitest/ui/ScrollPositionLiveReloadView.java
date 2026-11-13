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

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ScrollPositionLiveReloadView", layout = ViewTestLayout.class)
@RouteAlias(value = ScrollPositionLiveReloadView.FORWARDED_ROUTE, layout = ViewTestLayout.class)
public class ScrollPositionLiveReloadView extends AbstractLiveReloadView
        implements BeforeEnterObserver {

    public static final String RENDER_LATE_PARAMETER = "render-late";

    // The same view at another address, so that its scroll containers can be
    // found there as well
    public static final String FORWARDED_ROUTE = "com.vaadin.flow.uitest.ui.ScrollPositionLiveReloadView/forwarded";

    private static final String FORWARD_NEXT_LOAD_ATTRIBUTE = ScrollPositionLiveReloadView.class
            .getName() + ".forwardNextLoad";

    // Long enough for the Flow client to have processed the initial UIDL and
    // reported being idle before the containers can be scrolled
    private static final int RENDER_LATE_DELAY_MS = 2000;

    @JsDefinition
    public interface RenderLateJs extends Serializable {

        @JsExpression("setTimeout(() => Array.from(this.children).forEach(child => child.style.display = ''), $0)")
        void showChildrenLater(int delayMs);
    }

    private final Div outerScroll = new Div();

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

        // Makes the next load of the view forward to another URL, as a
        // reload that ends up on another page would
        NativeButton forwardButton = new NativeButton("Forward the next load",
                e -> VaadinSession.getCurrent()
                        .setAttribute(FORWARD_NEXT_LOAD_ATTRIBUTE, true));
        forwardButton.setId("forward-next-load-button");
        add(forwardButton);

        // Outer scrollable container
        outerScroll.setId("outer-scroll");
        outerScroll.getStyle().set("height", "400px");
        // Kept when the content is hidden, so that the container can be clicked
        outerScroll.getStyle().set("width", "600px");
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinSession session = event.getUI().getSession();
        if (session.getAttribute(FORWARD_NEXT_LOAD_ATTRIBUTE) != null) {
            session.setAttribute(FORWARD_NEXT_LOAD_ATTRIBUTE, null);
            event.forwardTo(FORWARDED_ROUTE);
            return;
        }
        if (event.getLocation().getQueryParameters()
                .getSingleParameter(RENDER_LATE_PARAMETER).isPresent()) {
            // Simulates a view that renders after the initial UIDL. The
            // containers cannot be scrolled until their content shows up,
            // while the window can, so that a restore is left half done.
            // Their own height is fixed, so the window position does not
            // move when the content appears.
            outerScroll.getChildren().forEach(child -> child.getElement()
                    .getStyle().setDisplay(Style.Display.NONE));
            outerScroll.getElement().executeJs(RenderLateJs.class)
                    .showChildrenLater(RENDER_LATE_DELAY_MS);
        }
    }
}
