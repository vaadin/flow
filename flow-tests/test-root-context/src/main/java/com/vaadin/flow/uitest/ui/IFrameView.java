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

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
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
    public IFrame frameResource = new IFrame();

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

        StreamResource dynamicResource = new StreamResource("dynamic-resource",
                () -> new ByteArrayInputStream(
                        "<html><body><span id=\"content\">Dynamic</span></body></html>"
                                .getBytes()));
        dynamicResource.setContentType("text/html");
        frameResource.setSrc(dynamicResource);
        frameResource.setId("frame2");

        add(frame, button, frameResource);
    }

    public void handleButtonClick() {
        content = "B";
        frame.reload();
    }

}
