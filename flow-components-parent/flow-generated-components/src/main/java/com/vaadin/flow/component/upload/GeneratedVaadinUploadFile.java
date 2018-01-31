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
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code row}</td>
 * <td>File container</td>
 * </tr>
 * <tr>
 * <td>{@code info}</td>
 * <td>Container for file status icon, file name, status and error messages</td>
 * </tr>
 * <tr>
 * <td>{@code done-icon}</td>
 * <td>File done status icon</td>
 * </tr>
 * <tr>
 * <td>{@code warning-icon}</td>
 * <td>File warning status icon</td>
 * </tr>
 * <tr>
 * <td>{@code meta}</td>
 * <td>Container for file name, status and error messages</td>
 * </tr>
 * <tr>
 * <td>{@code name}</td>
 * <td>File name</td>
 * </tr>
 * <tr>
 * <td>{@code error}</td>
 * <td>Error message, shown when error happens</td>
 * </tr>
 * <tr>
 * <td>{@code status}</td>
 * <td>Status message</td>
 * </tr>
 * <tr>
 * <td>{@code commands}</td>
 * <td>Container for file command icons</td>
 * </tr>
 * <tr>
 * <td>{@code start-button}</td>
 * <td>Start file upload button</td>
 * </tr>
 * <tr>
 * <td>{@code retry-button}</td>
 * <td>Retry file upload button</td>
 * </tr>
 * <tr>
 * <td>{@code clear-button}</td>
 * <td>Clear file button</td>
 * </tr>
 * <tr>
 * <td>{@code progress}</td>
 * <td>Progress bar</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The following state attributes are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Part name</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code error}</td>
 * <td>An error has happened during uploading</td>
 * <td>{@code progress}</td>
 * </tr>
 * <tr>
 * <td>{@code indeterminate}</td>
 * <td>Uploading is in progress, but the progress value is unknown</td>
 * <td>{@code progress}</td>
 * </tr>
 * <tr>
 * <td>{@code uploading}</td>
 * <td>Uploading is in progress</td>
 * <td>{@code progress}</td>
 * </tr>
 * <tr>
 * <td>{@code complete}</td>
 * <td>Uploading has finished successfully</td>
 * <td>{@code progress}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.UploadFileElement#UNKNOWN", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-upload-file")
@HtmlImport("frontend://bower_components/vaadin-upload/src/vaadin-upload-file.html")
public abstract class GeneratedVaadinUploadFile<R extends GeneratedVaadinUploadFile<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code file} property from the webcomponent
     */
    protected JsonObject getFileJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("file");
    }

    /**
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
    protected Registration addFileAbortListener(
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
    protected Registration addFileRemoveListener(
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
    protected Registration addFileRetryListener(
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
    protected Registration addFileStartListener(
            ComponentEventListener<FileStartEvent<R>> listener) {
        return addListener(FileStartEvent.class,
                (ComponentEventListener) listener);
    }
}