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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;

@Route("com.vaadin.flow.uitest.ui.UploadView")
public class UploadView extends Div {
    public static final String INPUT_ID = "customUploadInput";
    public static final String RESULT_ID = "result-span";
    public static final String UPLOAD_ID = "upload-button";
    public static final String REFRESH_ID = "refresh-button";

    public UploadView() {
        Span result = new Span("--empty--");
        result.setId(RESULT_ID);
        NativeButton button = new NativeButton("refresh from server", event -> {
        });
        button.setId(REFRESH_ID);
        add(result, new Div(), button);

        UploadHandler upload = (event) -> {
            try (InputStream inputStream = event.getInputStream()) {
                String content = IOUtils.toString(inputStream,
                        StandardCharsets.UTF_8);
                event.getUI().access(() -> result.setText(content));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        StreamResourceRegistry resourceRegistry = VaadinSession.getCurrent()
                .getResourceRegistry();
        StreamRegistration streamRegistration = resourceRegistry
                .registerResource(upload);

        String uploadUrl = resourceRegistry
                .getTargetURI(streamRegistration.getResource()).toString();

        Div container = new Div();
        Input input = new Input();
        input.setType("file");
        input.setId(INPUT_ID);
        container.add(input);

        NativeButton uploadButton = new NativeButton("Upload",
                (event) -> event.getSource().getElement().executeJs("""
                        const file = $0.files[0];
                        if (!file) {
                            alert("No file selected");
                            return;
                        }
                        const formData = new FormData();
                        formData.append("file", file);
                        fetch($1, {
                            method: "POST",
                            body: formData
                        }).then(response => {
                            const result = document.createElement("div");
                            if (response.ok) {
                                result.innerText = "Upload successful";
                            } else {
                                result.innerText = "Upload failed";
                            }
                            $0.parentElement.parentElement.appendChild(result);
                        });
                        """, input.getElement(), uploadUrl));
        uploadButton.setId(UPLOAD_ID);
        add(new Span(container, uploadButton));
    }
}
