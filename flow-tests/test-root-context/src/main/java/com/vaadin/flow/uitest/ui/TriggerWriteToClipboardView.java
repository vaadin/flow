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

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.ClipboardContent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.ImageBlobInput;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SignalInput;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Buttons exercising {@link WriteToClipboardAction}, grouped into sections so
 * the view doubles as a manual smoke-test page. The sections cover text inputs
 * (an input field's current value, a literal string with escape characters,
 * combined plain text and HTML), a server-side {@link ValueSignal}, and the
 * three image flavours: an {@link Image} already on the page, the same image
 * packed with text into one {@link ClipboardContent}, and a separate image
 * served by a {@link DownloadHandler} (intentionally a different colour so
 * pasted output makes it obvious which button was used). Each action's
 * success/error consumers write the outcome into the status {@link Div}. The IT
 * replaces {@code navigator.clipboard.write} and {@code ClipboardItem} with
 * recording shims so the assertions don't depend on browser clipboard
 * permissions; manual users can paste into any external app to verify.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerWriteToClipboardView", layout = ViewTestLayout.class)
public class TriggerWriteToClipboardView extends AbstractDivView {

    /** Literal copied by the {@code #copy-static} button. */
    static final String STATIC_TEXT = "hello \"world\"\n";

    /** Text slot copied by the {@code #copy-multi} button. */
    static final String MULTI_TEXT = "plain";

    /** HTML slot copied by the {@code #copy-multi} button. */
    static final String MULTI_HTML = "<b>html</b>";

    /**
     * Initial value of the signal copied by the {@code #copy-signal} button.
     */
    static final String SIGNAL_INITIAL_TEXT = "signal initial";

    /**
     * Value the signal is mutated to when the {@code #change-signal} button is
     * clicked.
     */
    static final String SIGNAL_UPDATED_TEXT = "signal updated";

    /** Red 32x32 PNG used by the in-DOM Image source. */
    static final byte[] PNG_BYTES = generatePng(Color.RED);

    /**
     * Blue 32x32 PNG served by the DownloadHandler — different colour from
     * {@link #PNG_BYTES} so a manual paste makes it obvious which button was
     * used.
     */
    static final byte[] HANDLER_PNG_BYTES = generatePng(Color.BLUE);

    /** Data-URL form of {@link #PNG_BYTES} for the in-DOM Image source. */
    static final String PNG_DATA_URL = "data:image/png;base64,"
            + Base64.getEncoder().encodeToString(PNG_BYTES);

    private static byte[] generatePng(Color color) {
        try {
            BufferedImage img = new BufferedImage(32, 32,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(color);
            g.fillRect(0, 0, 32, 32);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    protected void onShow() {
        Div status = new Div();
        status.setId("status");

        // --- Text sections ---------------------------------------------

        Input field = new Input();
        field.setId("source");
        NativeButton copyButton = new NativeButton("Copy input value");
        copyButton.setId("copy");
        addSection("Copy input value as text/plain",
                "Pastes the current value of the input field below.", field,
                copyButton);

        NativeButton copyStaticButton = new NativeButton("Copy static text");
        copyStaticButton.setId("copy-static");
        addSection("Copy a literal string as text/plain",
                "Pastes the literal: " + STATIC_TEXT.replace("\n", "\\n")
                        + " (quotes and trailing newline included).",
                copyStaticButton);

        NativeButton copyMultiButton = new NativeButton("Copy text + html");
        copyMultiButton.setId("copy-multi");
        addSection("Copy plain text and HTML together",
                "Pastes \"" + MULTI_TEXT + "\" into a plain editor or "
                        + MULTI_HTML + " into a rich-text editor.",
                copyMultiButton);

        // --- Signal section --------------------------------------------

        NativeButton copySignalButton = new NativeButton("Copy signal value");
        copySignalButton.setId("copy-signal");
        NativeButton changeSignalButton = new NativeButton(
                "Change signal value");
        changeSignalButton.setId("change-signal");
        Span signalDisplay = new Span();
        signalDisplay.setId("signal-value");
        addSection("Copy a server-side signal value as text/plain",
                "Pastes the signal's current value. Use \"Change signal value\""
                        + " to mutate it; the value below updates in lockstep"
                        + " with what the next copy click will produce.",
                copySignalButton, changeSignalButton, new Span(" current: "),
                signalDisplay);

        // --- Image sections --------------------------------------------

        Image sourceImage = new Image(PNG_DATA_URL, "test source");
        sourceImage.setId("source-image");
        NativeButton copyImageButton = new NativeButton("Copy image");
        copyImageButton.setId("copy-image");
        addSection("Copy an <img> already on the page as image/png",
                "Pastes this red square into an image-capable target"
                        + " (e.g. a chat app or document editor).",
                sourceImage, copyImageButton);

        NativeButton copyMultiImageButton = new NativeButton(
                "Copy text + image");
        copyMultiImageButton.setId("copy-multi-image");
        addSection("Copy text and an image into one ClipboardItem",
                "Pastes \"" + MULTI_TEXT
                        + "\" into a plain editor or the red square (same"
                        + " <img> as above) into an image-capable target.",
                copyMultiImageButton);

        NativeButton copyAllSlotsButton = new NativeButton(
                "Copy text + html + image");
        copyAllSlotsButton.setId("copy-all-slots");
        addSection("Copy all three slots into one ClipboardItem",
                "Pastes \"" + MULTI_TEXT + "\" into a plain editor, "
                        + MULTI_HTML
                        + " into a rich-text editor, or the red square into an"
                        + " image-capable target — whichever representation"
                        + " the paste target prefers.",
                copyAllSlotsButton);

        NativeButton copyImageHandlerButton = new NativeButton(
                "Copy image via handler");
        copyImageHandlerButton.setId("copy-image-handler");
        addSection("Copy a server-served image as image/png",
                "Pastes a blue square — different bytes from the red one"
                        + " above, served by a DownloadHandler on the server."
                        + " The <img> the binding creates is hidden, so what"
                        + " you copy is intentionally not visible on the page.",
                copyImageHandlerButton);

        // --- Outcome status --------------------------------------------

        add(new Hr(), new H2("Last action outcome"),
                new Paragraph("Each button's onCopied/onError callback writes"
                        + " here. \"ok:<value>\" means the write resolved;"
                        + " for image-only writes <value> is \"null\"."),
                status);

        // --- Wire up triggers ------------------------------------------

        Action.Input<String> value = new PropertyInput<>(field, "value",
                String.class);
        new ClickTrigger(copyButton).triggers(new WriteToClipboardAction(value,
                null, copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));

        new ClickTrigger(copyStaticButton).triggers(new WriteToClipboardAction(
                new LiteralInput<>(STATIC_TEXT), null,
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));

        new ClickTrigger(copyMultiButton).triggers(new WriteToClipboardAction(
                new LiteralInput<>(MULTI_TEXT), new LiteralInput<>(MULTI_HTML),
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));

        ValueSignal<String> textSignal = new ValueSignal<>(SIGNAL_INITIAL_TEXT);
        signalDisplay.getElement().bindText(textSignal);
        new ClickTrigger(copySignalButton).triggers(new WriteToClipboardAction(
                new SignalInput<>(this, textSignal), null,
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));
        changeSignalButton
                .addClickListener(e -> textSignal.set(SIGNAL_UPDATED_TEXT));

        new ClickTrigger(copyImageButton).triggers(new WriteToClipboardAction(
                new ImageBlobInput(sourceImage),
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message())));

        Clipboard.onClick(copyMultiImageButton).write(
                ClipboardContent.create().text(MULTI_TEXT).image(sourceImage),
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message()));

        Clipboard.onClick(copyAllSlotsButton).write(
                ClipboardContent.create().text(MULTI_TEXT).html(MULTI_HTML)
                        .image(sourceImage),
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message()));

        DownloadHandler imageHandler = DownloadHandler
                .fromInputStream(event -> new DownloadResponse(
                        new ByteArrayInputStream(HANDLER_PNG_BYTES),
                        "handler.png", "image/png", HANDLER_PNG_BYTES.length));
        Clipboard.onClick(copyImageHandlerButton).writeImage(imageHandler,
                copied -> status.setText("ok:" + copied), err -> status
                        .setText("err:" + err.name() + ":" + err.message()));
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
