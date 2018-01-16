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
import elemental.json.JsonArray;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-upload>} is a Polymer 2 element for uploading multiple files
 * with drag and drop support.
 * </p>
 * <p>
 * Example:
 * </p>
 * <p>
 * {@code <vaadin-upload></vaadin-upload>}
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
 * <td>{@code primary-buttons}</td>
 * <td>Upload container</td>
 * </tr>
 * <tr>
 * <td>{@code upload-button}</td>
 * <td>Upload button</td>
 * </tr>
 * <tr>
 * <td>{@code drop-label}</td>
 * <td>Label for drop indicator</td>
 * </tr>
 * <tr>
 * <td>{@code drop-label-icon}</td>
 * <td>Icon for drop indicator</td>
 * </tr>
 * <tr>
 * <td>{@code file-list}</td>
 * <td>File list container</td>
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
 * <td>{@code nodrop}</td>
 * <td>Set when drag and drop is disabled (e. g., on touch devices)</td>
 * <td>{@code :host}</td>
 * </tr>
 * <tr>
 * <td>{@code dragover}</td>
 * <td>A file is being dragged over the element</td>
 * <td>{@code :host}</td>
 * </tr>
 * <tr>
 * <td>{@code dragover-valid}</td>
 * <td>A dragged file is valid with {@code maxFiles} and {@code accept} criteria
 * </td>
 * <td>{@code :host}</td>
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
        "WebComponent: Vaadin.UploadElement#4.0.0-alpha3",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-upload")
@HtmlImport("frontend://bower_components/vaadin-upload/src/vaadin-upload.html")
public class GeneratedVaadinUpload<R extends GeneratedVaadinUpload<R>> extends
        Component implements HasStyle, ComponentSupplier<R>, HasComponents {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Define whether the element supports dropping files on it for uploading.
     * By default it's enabled in desktop and disabled in touch devices because
     * mobile devices do not support drag events in general. Setting it false
     * means that drop is enabled even in touch-devices, and true disables drop
     * in all devices.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code nodrop} property from the webcomponent
     */
    public boolean isNodrop() {
        return getElement().getProperty("nodrop", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Define whether the element supports dropping files on it for uploading.
     * By default it's enabled in desktop and disabled in touch devices because
     * mobile devices do not support drag events in general. Setting it false
     * means that drop is enabled even in touch-devices, and true disables drop
     * in all devices.
     * </p>
     * 
     * @param nodrop
     *            the boolean value to set
     */
    public void setNodrop(boolean nodrop) {
        getElement().setProperty("nodrop", nodrop);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The server URL. The default value is an empty string, which means that
     * <em>window.location</em> will be used.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code target} property from the webcomponent
     */
    public String getTarget() {
        return getElement().getProperty("target");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The server URL. The default value is an empty string, which means that
     * <em>window.location</em> will be used.
     * </p>
     * 
     * @param target
     *            the String value to set
     */
    public void setTarget(String target) {
        getElement().setProperty("target", target == null ? "" : target);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * HTTP Method used to send the files. Only POST and PUT are allowed.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code method} property from the webcomponent
     */
    public String getMethod() {
        return getElement().getProperty("method");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * HTTP Method used to send the files. Only POST and PUT are allowed.
     * </p>
     * 
     * @param method
     *            the String value to set
     */
    public void setMethod(String method) {
        getElement().setProperty("method", method == null ? "" : method);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Key-Value map to send to the server. If you set this property as an
     * attribute, use a valid JSON string, for example: {@code <vaadin-upload
     * headers=' "X-Foo": "Bar"}'></vaadin-upload>}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code headers} property from the webcomponent
     */
    protected JsonObject protectedGetHeaders() {
        return (JsonObject) getElement().getPropertyRaw("headers");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Key-Value map to send to the server. If you set this property as an
     * attribute, use a valid JSON string, for example: {@code <vaadin-upload
     * headers=' "X-Foo": "Bar"}'></vaadin-upload>}
     * </p>
     * 
     * @param headers
     *            the JsonObject value to set
     */
    protected void setHeaders(JsonObject headers) {
        getElement().setPropertyJson("headers", headers);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Max time in milliseconds for the entire upload process, if exceeded the
     * request will be aborted. Zero means that there is no timeout.
     * </p>
     * 
     * <pre>
     * <code>         &lt;p&gt;This property is not synchronized automatically from the client side, so the returned value may not be the same as in client side.
     * 	</code>
     * </pre>
     * 
     * @return the {@code timeout} property from the webcomponent
     */
    public double getTimeout() {
        return getElement().getProperty("timeout", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Max time in milliseconds for the entire upload process, if exceeded the
     * request will be aborted. Zero means that there is no timeout.
     * </p>
     * 
     * @param timeout
     *            the double value to set
     */
    public void setTimeout(double timeout) {
        getElement().setProperty("timeout", timeout);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The array of files being processed, or already uploaded.
     * </p>
     * <p>
     * Each element is a <a
     * href="https://developer.mozilla.org/en-US/docs/Web/API/File">{@code File}
     * </a> object with a number of extra properties to track the upload
     * process:
     * </p>
     * <ul>
     * <li>{@code uploadTarget}: The target URL used to upload this file.</li>
     * <li>{@code elapsed}: Elapsed time since the upload started.</li>
     * <li>{@code elapsedStr}: Human-readable elapsed time.</li>
     * <li>{@code remaining}: Number of seconds remaining for the upload to
     * finish.</li>
     * <li>{@code remainingStr}: Human-readable remaining time for the upload to
     * finish.</li>
     * <li>{@code progress}: Percentage of the file already uploaded.</li>
     * <li>{@code speed}: Upload speed in kB/s.</li>
     * <li>{@code size}: File size in bytes.</li>
     * <li>{@code totalStr}: Human-readable total size of the file.</li>
     * <li>{@code loaded}: Bytes transferred so far.</li>
     * <li>{@code loadedStr}: Human-readable uploaded size at the moment.</li>
     * <li>{@code status}: Status of the upload process.</li>
     * <li>{@code error}: Error message in case the upload failed.</li>
     * <li>{@code abort}: True if the file was canceled by the user.</li>
     * <li>{@code complete}: True when the file was transferred to the server.</li>
     * <li>{@code uploading}: True while trasferring data to the server.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'files-changed' event happens.</li>
     * </ul>
     * 
     * @return the {@code files} property from the webcomponent
     */
    @Synchronize(property = "files", value = "files-changed")
    protected JsonArray protectedGetFiles() {
        return (JsonArray) getElement().getPropertyRaw("files");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The array of files being processed, or already uploaded.
     * </p>
     * <p>
     * Each element is a <a
     * href="https://developer.mozilla.org/en-US/docs/Web/API/File">{@code File}
     * </a> object with a number of extra properties to track the upload
     * process:
     * </p>
     * <ul>
     * <li>{@code uploadTarget}: The target URL used to upload this file.</li>
     * <li>{@code elapsed}: Elapsed time since the upload started.</li>
     * <li>{@code elapsedStr}: Human-readable elapsed time.</li>
     * <li>{@code remaining}: Number of seconds remaining for the upload to
     * finish.</li>
     * <li>{@code remainingStr}: Human-readable remaining time for the upload to
     * finish.</li>
     * <li>{@code progress}: Percentage of the file already uploaded.</li>
     * <li>{@code speed}: Upload speed in kB/s.</li>
     * <li>{@code size}: File size in bytes.</li>
     * <li>{@code totalStr}: Human-readable total size of the file.</li>
     * <li>{@code loaded}: Bytes transferred so far.</li>
     * <li>{@code loadedStr}: Human-readable uploaded size at the moment.</li>
     * <li>{@code status}: Status of the upload process.</li>
     * <li>{@code error}: Error message in case the upload failed.</li>
     * <li>{@code abort}: True if the file was canceled by the user.</li>
     * <li>{@code complete}: True when the file was transferred to the server.</li>
     * <li>{@code uploading}: True while trasferring data to the server.</li>
     * </ul>
     * 
     * @param files
     *            the JsonArray value to set
     */
    protected void setFiles(JsonArray files) {
        getElement().setPropertyJson("files", files);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Limit of files to upload, by default it is unlimited. If the value is set
     * to one, native file browser will prevent selecting multiple files.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code maxFiles} property from the webcomponent
     */
    public double getMaxFiles() {
        return getElement().getProperty("maxFiles", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Limit of files to upload, by default it is unlimited. If the value is set
     * to one, native file browser will prevent selecting multiple files.
     * </p>
     * 
     * @param maxFiles
     *            the double value to set
     */
    public void setMaxFiles(double maxFiles) {
        getElement().setProperty("maxFiles", maxFiles);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies if the maximum number of files have been uploaded
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code maxFilesReached} property from the webcomponent
     */
    public boolean isMaxFilesReached() {
        return getElement().getProperty("maxFilesReached", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the types of files that the server accepts. Syntax: a
     * comma-separated list of MIME type patterns (wildcards are allowed) or
     * file extensions. Notice that MIME types are widely supported, while file
     * extensions are only implemented in certain browsers, so avoid using it.
     * Example: accept=&quot;video/*,image/tiff&quot; or
     * accept=&quot;.pdf,audio/mp3&quot;
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code accept} property from the webcomponent
     */
    public String getAccept() {
        return getElement().getProperty("accept");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the types of files that the server accepts. Syntax: a
     * comma-separated list of MIME type patterns (wildcards are allowed) or
     * file extensions. Notice that MIME types are widely supported, while file
     * extensions are only implemented in certain browsers, so avoid using it.
     * Example: accept=&quot;video/*,image/tiff&quot; or
     * accept=&quot;.pdf,audio/mp3&quot;
     * </p>
     * 
     * @param accept
     *            the String value to set
     */
    public void setAccept(String accept) {
        getElement().setProperty("accept", accept == null ? "" : accept);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the maximum file size in bytes allowed to upload. Notice that
     * it is a client-side constraint, which will be checked before sending the
     * request. Obviously you need to do the same validation in the server-side
     * and be sure that they are aligned.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code maxFileSize} property from the webcomponent
     */
    public double getMaxFileSize() {
        return getElement().getProperty("maxFileSize", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the maximum file size in bytes allowed to upload. Notice that
     * it is a client-side constraint, which will be checked before sending the
     * request. Obviously you need to do the same validation in the server-side
     * and be sure that they are aligned.
     * </p>
     * 
     * @param maxFileSize
     *            the double value to set
     */
    public void setMaxFileSize(double maxFileSize) {
        getElement().setProperty("maxFileSize", maxFileSize);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the 'name' property at Content-Disposition
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code formDataName} property from the webcomponent
     */
    public String getFormDataName() {
        return getElement().getProperty("formDataName");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the 'name' property at Content-Disposition
     * </p>
     * 
     * @param formDataName
     *            the String value to set
     */
    public void setFormDataName(String formDataName) {
        getElement().setProperty("formDataName",
                formDataName == null ? "" : formDataName);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Prevents upload(s) from immediately uploading upon adding file(s). When
     * set, you must manually trigger uploads using the {@code uploadFiles}
     * method
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noAuto} property from the webcomponent
     */
    public boolean isNoAuto() {
        return getElement().getProperty("noAuto", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Prevents upload(s) from immediately uploading upon adding file(s). When
     * set, you must manually trigger uploads using the {@code uploadFiles}
     * method
     * </p>
     * 
     * @param noAuto
     *            the boolean value to set
     */
    public void setNoAuto(boolean noAuto) {
        getElement().setProperty("noAuto", noAuto);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set the withCredentials flag on the request.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code withCredentials} property from the webcomponent
     */
    public boolean isWithCredentials() {
        return getElement().getProperty("withCredentials", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set the withCredentials flag on the request.
     * </p>
     * 
     * @param withCredentials
     *            the boolean value to set
     */
    public void setWithCredentials(boolean withCredentials) {
        getElement().setProperty("withCredentials", withCredentials);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The object used to localize this component. For changing the default
     * localization, change the entire <em>i18n</em> object or just the property
     * you want to modify.
     * </p>
     * <p>
     * The object has the following JSON structure and default values:
     * </p>
     * 
     * <pre>
     * <code>        {
     * 	          dropFiles: {
     * 	          one: 'Drop file here...',
     * 	          many: 'Drop files here...'
     * 	          },
     * 	          addFiles: {
     * 	          one: 'Select File',
     * 	          many: 'Upload Files'
     * 	          },
     * 	          cancel: 'Cancel',
     * 	          error: {
     * 	          tooManyFiles: 'Too Many Files.',
     * 	          fileIsTooBig: 'File is Too Big.',
     * 	          incorrectFileType: 'Incorrect File Type.'
     * 	          },
     * 	          uploading: {
     * 	          status: {
     * 	            connecting: 'Connecting...',
     * 	            stalled: 'Stalled.',
     * 	            processing: 'Processing File...',
     * 	            held: 'Queued'
     * 	          },
     * 	          remainingTime: {
     * 	            prefix: 'remaining time: ',
     * 	            unknown: 'unknown remaining time'
     * 	          },
     * 	          error: {
     * 	            serverUnavailable: 'Server Unavailable',
     * 	            unexpectedServerError: 'Unexpected Server Error',
     * 	            forbidden: 'Forbidden'
     * 	          }
     * 	          },
     * 	          units: {
     * 	          size: ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
     * 	          },
     * 	          formatSize: function(bytes) {
     * 	          // returns the size followed by the best suitable unit
     * 	          },
     * 	          formatTime: function(seconds, [secs, mins, hours]) {
     * 	          // returns a 'HH:MM:SS' string
     * 	          }
     * 	        }&lt;p&gt;This property is not synchronized automatically from the client side, so the returned value may not be the same as in client side.
     * 	</code>
     * </pre>
     * 
     * @return the {@code i18n} property from the webcomponent
     */
    protected JsonObject protectedGetI18n() {
        return (JsonObject) getElement().getPropertyRaw("i18n");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The object used to localize this component. For changing the default
     * localization, change the entire <em>i18n</em> object or just the property
     * you want to modify.
     * </p>
     * <p>
     * The object has the following JSON structure and default values:
     * </p>
     * 
     * <pre>
     * <code>        {
     * 	          dropFiles: {
     * 	          one: 'Drop file here...',
     * 	          many: 'Drop files here...'
     * 	          },
     * 	          addFiles: {
     * 	          one: 'Select File',
     * 	          many: 'Upload Files'
     * 	          },
     * 	          cancel: 'Cancel',
     * 	          error: {
     * 	          tooManyFiles: 'Too Many Files.',
     * 	          fileIsTooBig: 'File is Too Big.',
     * 	          incorrectFileType: 'Incorrect File Type.'
     * 	          },
     * 	          uploading: {
     * 	          status: {
     * 	            connecting: 'Connecting...',
     * 	            stalled: 'Stalled.',
     * 	            processing: 'Processing File...',
     * 	            held: 'Queued'
     * 	          },
     * 	          remainingTime: {
     * 	            prefix: 'remaining time: ',
     * 	            unknown: 'unknown remaining time'
     * 	          },
     * 	          error: {
     * 	            serverUnavailable: 'Server Unavailable',
     * 	            unexpectedServerError: 'Unexpected Server Error',
     * 	            forbidden: 'Forbidden'
     * 	          }
     * 	          },
     * 	          units: {
     * 	          size: ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
     * 	          },
     * 	          formatSize: function(bytes) {
     * 	          // returns the size followed by the best suitable unit
     * 	          },
     * 	          formatTime: function(seconds, [secs, mins, hours]) {
     * 	          // returns a 'HH:MM:SS' string
     * 	          }
     * 	        }
     * 	</code>
     * </pre>
     * 
     * @param i18n
     *            the JsonObject value to set
     */
    protected void setI18n(JsonObject i18n) {
        getElement().setPropertyJson("i18n", i18n);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Triggers the upload of any files that are not completed
     * </p>
     * 
     * @param files
     *            Missing documentation!
     */
    protected void uploadFiles(JsonObject files) {
        getElement().callFunction("uploadFiles", files);
    }

    @DomEvent("file-reject")
    public static class FileRejectEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailFile;
        private final JsonObject detailError;

        public FileRejectEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.file") JsonObject detailFile,
                @EventData("event.detail.error") JsonObject detailError) {
            super(source, fromClient);
            this.detail = detail;
            this.detailFile = detailFile;
            this.detailError = detailError;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }

        public JsonObject getDetailError() {
            return detailError;
        }
    }

    /**
     * Adds a listener for {@code file-reject} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFileRejectListener(
            ComponentEventListener<FileRejectEvent<R>> listener) {
        return addListener(FileRejectEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-abort")
    public static class UploadAbortEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadAbortEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-abort} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadAbortListener(
            ComponentEventListener<UploadAbortEvent<R>> listener) {
        return addListener(UploadAbortEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-before")
    public static class UploadBeforeEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;
        private final JsonObject detailFileUploadTarget;

        public UploadBeforeEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile,
                @EventData("event.detail.file.uploadTarget") JsonObject detailFileUploadTarget) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
            this.detailFileUploadTarget = detailFileUploadTarget;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }

        public JsonObject getDetailFileUploadTarget() {
            return detailFileUploadTarget;
        }
    }

    /**
     * Adds a listener for {@code upload-before} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadBeforeListener(
            ComponentEventListener<UploadBeforeEvent<R>> listener) {
        return addListener(UploadBeforeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-error")
    public static class UploadErrorEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadErrorEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-error} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadErrorListener(
            ComponentEventListener<UploadErrorEvent<R>> listener) {
        return addListener(UploadErrorEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-progress")
    public static class UploadProgressEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadProgressEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-progress} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadProgressListener(
            ComponentEventListener<UploadProgressEvent<R>> listener) {
        return addListener(UploadProgressEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-request")
    public static class UploadRequestEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;
        private final JsonObject detailFormData;

        public UploadRequestEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile,
                @EventData("event.detail.formData") JsonObject detailFormData) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
            this.detailFormData = detailFormData;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }

        public JsonObject getDetailFormData() {
            return detailFormData;
        }
    }

    /**
     * Adds a listener for {@code upload-request} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadRequestListener(
            ComponentEventListener<UploadRequestEvent<R>> listener) {
        return addListener(UploadRequestEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-response")
    public static class UploadResponseEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadResponseEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-response} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadResponseListener(
            ComponentEventListener<UploadResponseEvent<R>> listener) {
        return addListener(UploadResponseEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-retry")
    public static class UploadRetryEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadRetryEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-retry} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadRetryListener(
            ComponentEventListener<UploadRetryEvent<R>> listener) {
        return addListener(UploadRetryEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-start")
    public static class UploadStartEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadStartEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-start} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadStartListener(
            ComponentEventListener<UploadStartEvent<R>> listener) {
        return addListener(UploadStartEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("upload-success")
    public static class UploadSuccessEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        private final JsonObject detail;
        private final JsonObject detailXhr;
        private final JsonObject detailFile;

        public UploadSuccessEvent(R source, boolean fromClient,
                @EventData("event.detail") JsonObject detail,
                @EventData("event.detail.xhr") JsonObject detailXhr,
                @EventData("event.detail.file") JsonObject detailFile) {
            super(source, fromClient);
            this.detail = detail;
            this.detailXhr = detailXhr;
            this.detailFile = detailFile;
        }

        public JsonObject getDetail() {
            return detail;
        }

        public JsonObject getDetailXhr() {
            return detailXhr;
        }

        public JsonObject getDetailFile() {
            return detailFile;
        }
    }

    /**
     * Adds a listener for {@code upload-success} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addUploadSuccessListener(
            ComponentEventListener<UploadSuccessEvent<R>> listener) {
        return addListener(UploadSuccessEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("files-changed")
    public static class FilesChangeEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        public FilesChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code files-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addFilesChangeListener(
            ComponentEventListener<FilesChangeEvent<R>> listener) {
        return addListener(FilesChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("max-files-reached-changed")
    public static class MaxFilesReachedChangeEvent<R extends GeneratedVaadinUpload<R>>
            extends ComponentEvent<R> {
        public MaxFilesReachedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code max-files-reached-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addMaxFilesReachedChangeListener(
            ComponentEventListener<MaxFilesReachedChangeEvent<R>> listener) {
        return addListener(MaxFilesReachedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'add-button'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public R addToAddButton(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "add-button");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'drop-label-icon'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public R addToDropLabelIcon(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "drop-label-icon");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'drop-label'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public R addToDropLabel(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "drop-label");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'file-list'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public R addToFileList(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "file-list");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    @Override
    public void remove(Component... components) {
        for (Component component : components) {
            if (getElement().equals(component.getElement().getParent())) {
                component.getElement().removeAttribute("slot");
                getElement().removeChild(component.getElement());
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
    }

    @Override
    public void removeAll() {
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }

    /**
     * Adds the given components as children of this component.
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public GeneratedVaadinUpload(Component... components) {
        add(components);
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinUpload() {
    }
}