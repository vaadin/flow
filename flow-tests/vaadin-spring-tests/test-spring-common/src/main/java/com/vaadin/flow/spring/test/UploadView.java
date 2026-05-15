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
package com.vaadin.flow.spring.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;

@Route("multipart-upload")
public class UploadView extends Div {

    public UploadView() {
        UploadHandler handler = event -> {
            if (!event.getContentType().startsWith("text")) {
                event.reject("Only text uploads are supported");
                return;
            }
            String content;
            try (InputStream stream = event.getInputStream()) {
                content = IOUtils.toString(stream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                content = "exception reading stream";
            }
            String text = content;
            event.getUI().access(() -> {
                Div div = new Div();
                div.setText(text);
                div.addClassName("uploaded-text");
                add(div);
            });
        };

        StreamResourceRegistry resourceRegistry = VaadinSession.getCurrent()
                .getResourceRegistry();
        StreamRegistration registration = resourceRegistry
                .registerResource(handler);
        String uploadUrl = resourceRegistry
                .getTargetURI(registration.getResource()).toString();

        Input upload = new Input();
        upload.setType("file");
        upload.setId("upl");
        upload.getElement().executeJs("""
                this.addEventListener('change', () => {
                    const file = this.files[0];
                    if (!file) return;
                    const formData = new FormData();
                    formData.append('file', file);
                    fetch($0, { method: 'POST', body: formData });
                });
                """, uploadUrl);
        add(upload);
    }
}
