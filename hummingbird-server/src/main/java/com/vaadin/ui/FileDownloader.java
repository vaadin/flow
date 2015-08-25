/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import com.vaadin.server.ClassResource;
import com.vaadin.server.ConnectorResource;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.ThemeResource;

/**
 * Extension that starts a download when the extended component is clicked. This
 * is used to overcome two challenges:
 * <ul>
 * <li>Resource should be bound to a component to allow it to be garbage
 * collected when there are no longer any ways of reaching the resource.</li>
 * <li>Download should be started directly when the user clicks e.g. a Button
 * without going through a server-side click listener to avoid triggering
 * security warnings in some browsers.</li>
 * </ul>
 * <p>
 * Please note that the download will be started in an iframe, which means that
 * care should be taken to avoid serving content types that might make the
 * browser attempt to show the content using a plugin instead of downloading it.
 * Connector resources (e.g. {@link FileResource} and {@link ClassResource})
 * will automatically be served using a
 * <code>Content-Type: application/octet-stream</code> header unless
 * {@link #setOverrideContentType(boolean)} has been set to <code>false</code>
 * while files served in other ways, (e.g. {@link ExternalResource} or
 * {@link ThemeResource}) will not automatically get this treatment.
 * </p>
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public class FileDownloader {

    private Resource fileDownloadResource;
    private boolean overrideContentType = true;

    /**
     * Creates a new file downloader for the given resource. To use the
     * downloader, you should also {@link #extend(AbstractClientConnector)} the
     * component.
     *
     * @param resource
     *            the resource to download when the user clicks the extended
     *            component.
     */
    public FileDownloader(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource may not be null");
        }
        fileDownloadResource = resource;
    }

    /**
     * Gets the resource set for download.
     *
     * @return the resource that will be downloaded if clicking the extended
     *         component
     */
    public Resource getFileDownloadResource() {
        return fileDownloadResource;
    }

    /**
     * Sets the resource that is downloaded when the extended component is
     * clicked.
     *
     * @param resource
     *            the resource to download
     */
    public void setFileDownloadResource(Resource fileDownloadResource) {
        this.fileDownloadResource = fileDownloadResource;
    }

    /**
     * Sets whether the content type of served resources should be overriden to
     * <code>application/octet-stream</code> to reduce the risk of a browser
     * plugin choosing to display the resource instead of downloading it. This
     * is by default set to <code>true</code>.
     * <p>
     * Please note that this only affects Connector resources (e.g.
     * {@link FileResource} and {@link ClassResource}) but not other resource
     * types (e.g. {@link ExternalResource} or {@link ThemeResource}).
     * </p>
     *
     * @param overrideContentType
     *            <code>true</code> to override the content type if possible;
     *            <code>false</code> to use the original content type.
     */
    public void setOverrideContentType(boolean overrideContentType) {
        this.overrideContentType = overrideContentType;
    }

    /**
     * Checks whether the content type should be overridden.
     *
     * @see #setOverrideContentType(boolean)
     *
     * @return <code>true</code> if the content type will be overridden when
     *         possible; <code>false</code> if the original content type will be
     *         used.
     */
    public boolean isOverrideContentType() {
        return overrideContentType;
    }

    public static class DownloadResource implements ConnectorResource {

        private ConnectorResource originalResource;
        private boolean overrideContentType;

        public DownloadResource(ConnectorResource originalResource,
                boolean overrideContentType) {
            this.originalResource = originalResource;
            this.overrideContentType = overrideContentType;
        }

        @Override
        public String getMIMEType() {
            return originalResource.getMIMEType();
        }

        @Override
        public DownloadStream getStream() {
            DownloadStream stream = originalResource.getStream();
            //
            String contentDisposition = stream
                    .getParameter(DownloadStream.CONTENT_DISPOSITION);
            if (contentDisposition == null) {
                contentDisposition = "attachment; " + DownloadStream
                        .getContentDispositionFilename(stream.getFileName());
                stream.setParameter(DownloadStream.CONTENT_DISPOSITION,
                        contentDisposition);
            }

            // Content-Type to block eager browser plug-ins from hijacking
            // the file
            if (overrideContentType) {
                stream.setContentType("application/octet-stream;charset=UTF-8");
            }

            return stream;
        }

        @Override
        public String getFilename() {
            return originalResource.getFilename();
        }

    }

    public void attach(AbstractComponent c) {
        String url;

        if (fileDownloadResource instanceof ConnectorResource) {
            // Served by servlet
            String key = "download" + hashCode();
            DownloadResource downloadResource = new DownloadResource(
                    (ConnectorResource) fileDownloadResource,
                    isOverrideContentType());
            c.setResource(key, downloadResource);
            url = new ResourceReference(downloadResource, c, key).getURL();
            // Hack, wrong in many cases
            url = url.replace("app://", "./");
        } else if (fileDownloadResource instanceof ExternalResource) {
            url = ((ExternalResource) fileDownloadResource).getURL();
        } else {
            throw new IllegalArgumentException(
                    "Only ConnectorResource and ExternalResource are supported");
        }

        c.getUI().getRootNode().enqueueRpc(c.getElement().getNode(),
                "$0.addEventListener('click', function(e) {"//
                        + "if (false) { " // Browser.isIOS()
                        + " window.open($1, '_blank','');"//
                        + "} else {"
                        + " var iframe = document.createElement('iframe');"
                        + " iframe.style.visibility='hidden';"
                        + " iframe.style.height='0px';"
                        + " iframe.style.width='0px';"
                        + " iframe.frameBorder=0;"//
                        + " iframe.tabIndex=-1;" //
                        + " iframe.src=$1;"
                        + " document.body.appendChild(iframe);"
                        + " if (!$0.downloadFrames) {"
                        + " $0.downloadFrames = [];" //
                        + " }" //
                        + " $0.downloadFrames.push(iframe);"
                        // + " // TODO remove all downloadFrames when $0 is
                        // detached"
                        + "}"//
                        + "});",
                c.getElement(), url);

    }

}
