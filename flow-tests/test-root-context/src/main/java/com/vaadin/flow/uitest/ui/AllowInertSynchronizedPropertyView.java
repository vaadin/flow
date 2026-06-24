/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeDetails;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.AllowInertSynchronizedPropertyView")
public class AllowInertSynchronizedPropertyView extends AbstractDivView {

    public static final String OPEN_MODAL_BUTTON = "modal-dialog-button";
    public static final String READ_NATIVE_DETAILS_STATE_BUTTON = "read-native-details-state-button";
    public static final String NATIVE_DETAILS_STATE = "native-details-state";
    public static final String NATIVE_DETAILS_SUMMARY = "native-details-summary";

    private NativeDetails nativeDetails;
    private Span state;

    @Override
    protected void onShow() {
        add(createOpenDialogButton(OPEN_MODAL_BUTTON));

        nativeDetails = new NativeDetails();
        add(nativeDetails);

        Span summary = new Span("Native details summary");
        summary.setId(NATIVE_DETAILS_SUMMARY);
        nativeDetails.setSummary(summary);

        state = new Span("unknown");
        state.setId(NATIVE_DETAILS_STATE);
        add(state);
    }

    private Component createOpenDialogButton(String id) {
        final NativeButton button = createButton("Open modal dialog",
                event -> new Dialog().open());
        button.setId(id);
        return button;
    }

    private NativeButton createButton(String caption,
            ComponentEventListener<ClickEvent<NativeButton>> listener) {
        final NativeButton button = new NativeButton();
        button.setText(caption);
        button.addClickListener(listener);
        button.getStyle().set("border", "1px solid black");
        button.setWidth("100px");
        return button;
    }

    public class Dialog extends Div {

        public Dialog() {
            final NativeButton readNativeDetailsStateButton = new NativeButton(
                    "Read Native Details State", event -> {
                        if (nativeDetails.isOpen()) {
                            state.setText("opened");
                        } else {
                            state.setText("closed");
                        }
                    });
            readNativeDetailsStateButton
                    .setId(READ_NATIVE_DETAILS_STATE_BUTTON);

            add(new Text("A modal dialog"), readNativeDetailsStateButton);

            getStyle().set("position", "fixed").set("inset", "50% 50%")
                    .set("border", "1px solid black");
        }

        public void open() {
            final UI ui = UI.getCurrent();
            ui.addModal(this);
        }
    }
}
