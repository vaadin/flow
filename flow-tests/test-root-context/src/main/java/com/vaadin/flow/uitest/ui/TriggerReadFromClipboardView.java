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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.ClipboardBinding;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Buttons exercising {@link ClipboardBinding}'s read methods, grouped into
 * sections so the view doubles as a manual smoke-test page: a multi-format read
 * returning both {@code text/plain} and {@code text/html}, a text-only read,
 * and an html-only read. Each action's payload/error consumer writes the
 * outcome into the status {@link Div}. The IT replaces
 * {@code navigator.clipboard.read} with a recording shim so the assertions
 * don't depend on browser clipboard permissions; manual users copy something
 * into their system clipboard first, then click a button and read the result in
 * the status div.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerReadFromClipboardView", layout = ViewTestLayout.class)
public class TriggerReadFromClipboardView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div status = new Div();
        status.setId("status");

        NativeButton readButton = new NativeButton("Read clipboard");
        readButton.setId("read");
        addSection("Read both text/plain and text/html",
                "Pastes the current clipboard's text and html into the status"
                        + " line as \"text=...;html=...\" (or \"null\" if the"
                        + " clipboard is empty).",
                readButton);

        NativeButton readTextButton = new NativeButton("Read text only");
        readTextButton.setId("read-text");
        addSection("Read only text/plain",
                "Pastes just the clipboard's text/plain field into the status"
                        + " line as \"text=...\" (or \"text=null\" if absent).",
                readTextButton);

        NativeButton readHtmlButton = new NativeButton("Read html only");
        readHtmlButton.setId("read-html");
        addSection("Read only text/html",
                "Pastes just the clipboard's text/html field into the status"
                        + " line as \"html=...\" (or \"html=null\" if absent).",
                readHtmlButton);

        add(new Hr(), new H2("Last action outcome"),
                new Paragraph("Each button's onPayload/onError callback writes"
                        + " here. \"error=<name>\" appears when the browser"
                        + " rejected the read (e.g. permission denied)."),
                status);

        Clipboard.onClick(readButton).read(p -> {
            if (p == null) {
                status.setText("null");
            } else {
                status.setText("text=" + p.text() + ";html=" + p.html());
            }
        }, err -> status.setText("error=" + err.name()));

        Clipboard.onClick(readTextButton).readText(
                text -> status.setText("text=" + text),
                err -> status.setText("error=" + err.name()));

        Clipboard.onClick(readHtmlButton).readHtml(
                html -> status.setText("html=" + html),
                err -> status.setText("error=" + err.name()));
    }

    private void addSection(String heading, String description,
            Component... contents) {
        Div section = new Div();
        section.getStyle().set("margin", "1em 0").set("padding", "0.5em 0")
                .set("border-top", "1px solid #ccc");
        section.add(new H2(heading));
        section.add(new Paragraph(description));
        section.add(contents);
        add(section);
    }
}
