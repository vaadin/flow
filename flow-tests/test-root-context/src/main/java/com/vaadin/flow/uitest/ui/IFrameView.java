/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * View for testing IFrame.reload(), based on
 * https://github.com/vaadin/flow/issues/6808
 *
 * @since 14.0
 */
@Route(value = "com.vaadin.flow.uitest.ui.IFrameView", layout = ViewTestLayout.class)
public class IFrameView extends AbstractDivView {

    private static String content = "A";

    @Route(value = "iframecontent")
    public static class IFrameContentView extends AbstractDivView {
        public IFrameContentView() {
            Span span = new Span(content);
            span.setId("Friday");
            add(span);
        }
    }

    public IFrame frame = new IFrame();
    public NativeButton button;

    public IFrameView() {
        content = "A";
        /*
         * The test consists of creating a view with an IFrame and a button. The
         * IFrame contains a span, which contains text "A". Upon pressing the
         * button, "B" is loaded into the span. The test then verifies that "B"
         * is visible in the span.
         */
        frame.setSrc("/view/iframecontent");
        frame.setId("frame1");
        button = new NativeButton("Reload", event -> handleButtonClick());
        button.setId("Reload");

        button.addClickListener(event -> handleButtonClick());
        add(frame, button);

    }

    public void handleButtonClick() {
        content = "B";
        frame.reload();
    }

}
