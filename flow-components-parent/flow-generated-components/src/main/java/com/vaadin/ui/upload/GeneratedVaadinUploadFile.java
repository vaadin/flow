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
package com.vaadin.ui.upload;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.event.EventData;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-upload-file>} element represents a file in the file list of
 * {@code <vaadin-upload>}.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following custom properties are available for styling the component.
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --vaadin-upload-file}</td>
 * <td>A mixin that is applied to the host element</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-row}</td>
 * <td>A mixin that is applied to the file row</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-status-icon}</td>
 * <td>A mixin that is applied to all file status icons. By default, file status
 * icons are hidden until the upload process finishes.</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-status-icon-complete}</td>
 * <td>A mixin that is applied to the complete status icon when the upload
 * process succeeds</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-status-icon-error}</td>
 * <td>A mixin that is applied to the error status icon when the upload process
 * fails</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-meta}</td>
 * <td>A mixin that is applied to the info container</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-meta-complete}</td>
 * <td>A mixin that is applied to the info container when file upload is
 * complete</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-meta-error}</td>
 * <td>A mixin that is applied to the info container when an error happens</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-name}</td>
 * <td>A mixin that is applied to the file name</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-status}</td>
 * <td>A mixin that is applied to the file status label</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-error}</td>
 * <td>A mixin that is applied to the file error label</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-commands}</td>
 * <td>A mixin that is applied to the buttons container</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-progress}</td>
 * <td>A mixin that is applied to the included paper-progress</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-progress-error}</td>
 * <td>A mixin that is applied to the progress bar when error is set</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-progress-indeterminate}</td>
 * <td>A mixin that is applied to the progress bar when indeterminate</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-progress-uploading-indeterminate}</td>
 * <td>A mixin that is applied to the progress bar when uploading and
 * indeterminate</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-progress-complete}</td>
 * <td>A mixin that is applied to the progress when file is complete</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-upload-file-canceled}</td>
 * <td>A mixin that is applied to the upload cancel animation</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: vaadin-upload-file#2.1.2", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-upload-file")
@HtmlImport("frontend://bower_components/vaadin-upload/vaadin-upload-file.html")
public class GeneratedVaadinUploadFile<R extends GeneratedVaadinUploadFile<R>>
        extends Component implements ComponentSupplier<R>, HasStyle {

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
    protected void setFile(elemental.json.JsonObject file) {
        getElement().setPropertyJson("file", file);
    }

    @DomEvent("file-abort")
    public static class FileAbortEvent<R extends GeneratedVaadinUploadFile<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailFile;

        public FileAbortEvent(R source, boolean fromClient,
                @EventData("event.detail") elemental.json.JsonObject detail,
                @EventData("event.detail.file") elemental.json.JsonObject detailFile) {
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
                @EventData("event.detail") elemental.json.JsonObject detail,
                @EventData("event.detail.file") elemental.json.JsonObject detailFile) {
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
                @EventData("event.detail") elemental.json.JsonObject detail,
                @EventData("event.detail.file") elemental.json.JsonObject detailFile) {
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
                @EventData("event.detail") elemental.json.JsonObject detail,
                @EventData("event.detail.file") elemental.json.JsonObject detailFile) {
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

    public Registration addFileStartListener(
            ComponentEventListener<FileStartEvent<R>> listener) {
        return addListener(FileStartEvent.class,
                (ComponentEventListener) listener);
    }
}