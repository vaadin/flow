/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;

@Route("resync-loop")
public class ResyncLoopView extends Div {

    public static final String ACCESS_MODE = "accessMode";
    public static final String ACCESS_MODE_ACCESS = "accessModeAccess";
    public static final String ACCESS_MODE_ACCESS_SYNC = "accessModeAccessSync";
    public static final String FORCE_RESYNC = "forceResync";
    public static final String USELESS_BUTTON = "uselessButton";
    public static final String JS_CALLBACK = "jsCallback";
    public static final String BG_CALLBACK = "bgCallback";
    private final Span jsCallbackSpan;
    private final Span bgCallbackSpan;
    private final Span accessModeSpan;
    private final AtomicInteger jsCallbackCounter = new AtomicInteger();
    private final AtomicInteger bgCallbackCounter = new AtomicInteger();
    ExecutorService executorService = Executors.newCachedThreadPool();
    AccessMode accessMode = AccessMode.accessSynchronously;

    public ResyncLoopView() {
        getStyle().setDisplay(Style.Display.FLEX);
        getStyle().setFlexDirection(Style.FlexDirection.COLUMN);
        getStyle().setAlignItems(Style.AlignItems.FLEX_START);
        UI ui = UI.getCurrent();
        jsCallbackSpan = new Span();
        jsCallbackSpan.setId(JS_CALLBACK);
        bgCallbackSpan = new Span();
        bgCallbackSpan.setId(BG_CALLBACK);
        accessModeSpan = new Span();
        accessModeSpan.setId(ACCESS_MODE);
        updateAccessMode(accessMode);
        NativeButton accessModeAccessButton = new NativeButton(
                "Set access mode to " + AccessMode.access,
                ev -> updateAccessMode(AccessMode.access));
        accessModeAccessButton.setId(ACCESS_MODE_ACCESS);
        NativeButton accessModeAccessSyncButton = new NativeButton(
                "Set access mode to " + AccessMode.accessSynchronously,
                ev -> updateAccessMode(AccessMode.accessSynchronously));
        accessModeAccessSyncButton.setId(ACCESS_MODE_ACCESS_SYNC);
        NativeButton forceResyncButton = new NativeButton("Force resync",
                ev -> {
                    jsCallbackSpan.setVisible(true);
                    ui.getInternals().incrementServerId();
                });
        forceResyncButton.setId(FORCE_RESYNC);
        NativeButton uselessButton = new NativeButton("Useless button", ev -> {
        });

        uselessButton.setId(USELESS_BUTTON);
        add(accessModeSpan, accessModeAccessButton, accessModeAccessSyncButton,
                forceResyncButton, uselessButton, jsCallbackSpan,
                bgCallbackSpan);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        CustomServlet.slowDownResponse();
        UI ui = attachEvent.getUI();

        executorService.execute(() -> {
            Command action = () -> bgCallbackSpan
                    .setText("Background update completed: "
                            + bgCallbackCounter.incrementAndGet());
            if (accessMode == AccessMode.access) {
                ui.access(action);
            } else {
                ui.accessSynchronously(action);
            }
        });
        ui.getElement().executeJs("return true;")
                .then(val -> jsCallbackSpan.setText("JS Callback completed: "
                        + jsCallbackCounter.incrementAndGet()));
    }

    private void updateAccessMode(AccessMode accessMode) {
        this.accessMode = accessMode;
        this.accessModeSpan.setText("Access mode: " + accessMode);
    }

    private enum AccessMode {
        access, accessSynchronously
    }
}
