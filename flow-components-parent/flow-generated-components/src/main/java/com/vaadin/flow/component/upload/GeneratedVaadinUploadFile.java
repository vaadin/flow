/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component.upload;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: vaadin-upload-file#2.1.2", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-upload-file")
@HtmlImport("frontend://bower_components/vaadin-upload/vaadin-upload-file.html")
public class GeneratedVaadinUploadFile<R extends GeneratedVaadinUploadFile<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * File metadata, upload status and progress information.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code file} property from the webcomponent
     */
    protected JsonObject protectedGetFile() {
        return (JsonObject) getElement().getPropertyRaw("file");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * File metadata, upload status and progress information.
     * </p>
     * 
     * @param file
     *            the JsonObject value to set
     */
    protected void setFile(JsonObject file) {
        getElement().setPropertyJson("file", file);
    }

    @DomEvent("file-abort")
    public static class FileAbortEvent<R extends GeneratedVaadinUploadFile<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailFile;

        public FileAbortEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code file-abort} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFileAbortListener(
            ComponentEventListener<FileAbortEvent<R>> listener) {
        return addListener(FileAbortEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("file-remove")
    public static class FileRemoveEvent<R extends GeneratedVaadinUploadFile<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailFile;

        public FileRemoveEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code file-remove} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFileRemoveListener(
            ComponentEventListener<FileRemoveEvent<R>> listener) {
        return addListener(FileRemoveEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("file-retry")
    public static class FileRetryEvent<R extends GeneratedVaadinUploadFile<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailFile;

        public FileRetryEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code file-retry} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFileRetryListener(
            ComponentEventListener<FileRetryEvent<R>> listener) {
        return addListener(FileRetryEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("file-start")
    public static class FileStartEvent<R extends GeneratedVaadinUploadFile<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailFile;

        public FileStartEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code file-start} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFileStartListener(
            ComponentEventListener<FileStartEvent<R>> listener) {
        return addListener(FileStartEvent.class,
                (ComponentEventListener) listener);
    }
}