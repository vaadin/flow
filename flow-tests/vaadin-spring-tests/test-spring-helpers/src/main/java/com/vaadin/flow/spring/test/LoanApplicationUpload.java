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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;

/**
 * The "upload your loan application" UI shared by the Spring Security test
 * apps. It uses a native {@code <input type="file">} that posts the selected
 * file as a raw XHR body with an {@code X-Filename} header to a session-scoped
 * {@link UploadHandler} (the servlets are not {@code @MultipartConfig}), and on
 * success shows a confirmation paragraph (id {@code uploadText}) and the
 * uploaded image (id {@code uploadImage}). It pulls no Vaadin component npm
 * packages.
 */
public final class LoanApplicationUpload {

    private LoanApplicationUpload() {
    }

    /**
     * Adds the loan application upload UI to the given view.
     *
     * @param view
     *            the view to add the upload components to
     * @param uploaderName
     *            supplies the name shown in the confirmation text, evaluated
     *            when an upload succeeds
     */
    public static void addTo(HasComponents view,
            Supplier<String> uploaderName) {
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        UploadHandler uploadHandler = event -> {
            try (InputStream inputStream = event.getInputStream()) {
                inputStream.transferTo(imageStream);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            event.getUI().access(() -> {
                Paragraph p = new Paragraph(
                        "Loan application uploaded by " + uploaderName.get());
                p.setId("uploadText");
                view.add(p);
                Image image = new Image(new StreamResource("image.png",
                        () -> new ByteArrayInputStream(
                                imageStream.toByteArray())),
                        "image");
                image.setId("uploadImage");
                view.add(image);
            });
        };

        StreamResourceRegistry resourceRegistry = VaadinSession.getCurrent()
                .getResourceRegistry();
        StreamRegistration streamRegistration = resourceRegistry
                .registerResource(uploadHandler);
        String uploadUrl = resourceRegistry
                .getTargetURI(streamRegistration.getResource()).toString();

        // The upload posts via fetch(), which does not piggyback Flow's UIDL
        // sync. Click a hidden button after the response to force a server
        // roundtrip that pulls down the queued DOM updates.
        NativeButton uploadSync = new NativeButton("", event -> {
        });
        uploadSync.getStyle().set("display", "none");

        Input uploadInput = new Input();
        uploadInput.setType("file");
        uploadInput.setId("uploadInput");
        uploadInput.getElement().executeJs(
                """
                        this.addEventListener('change', () => {
                            const file = this.files[0];
                            if (!file) return;
                            fetch($0, {
                                method: 'POST',
                                headers: {
                                    'Content-Type': file.type || 'application/octet-stream',
                                    'X-Filename': encodeURIComponent(file.name)
                                },
                                body: file
                            }).then(() => $1.click());
                        });
                        """,
                uploadUrl, uploadSync.getElement());

        view.add(new H4("Upload your loan application"), uploadInput,
                uploadSync);
    }
}
