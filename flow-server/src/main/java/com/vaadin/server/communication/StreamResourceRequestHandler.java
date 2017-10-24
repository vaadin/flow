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
package com.vaadin.server.communication;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceWriter;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 *
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRequestHandler {

    private static final char PATH_SEPARATOR = '/';

    /**
     * Dynamic resource URI prefix.
     */
    static final String DYN_RES_PREFIX = "VAADIN/dynamic/generated-resources/";

    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response, StreamResource streamResource)
            throws IOException {

        StreamResourceWriter writer = null;
        session.lock();
        try {
            ServletContext context = ((VaadinServletRequest) request)
                    .getServletContext();
            response.setContentType(streamResource.getContentTypeResolver()
                    .apply(streamResource, context));
            response.setCacheTime(streamResource.getCacheTime());
            writer = streamResource.getWriter();
            if (writer == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }
        } finally {
            session.unlock();
        }
        try (OutputStream outputStream = response.getOutputStream()) {
            writer.accept(outputStream, session);
        }
        return true;
    }

    /**
     * Generates URI string for a dynamic resource using its {@code id} and
     * {@code name}.
     *
     * @param id
     *            unique resource id
     * @param name
     *            resource name
     * @return generated URI string
     */
    public static String generateURI(String id, String name) {
        StringBuilder builder = new StringBuilder(DYN_RES_PREFIX);
        try {
            builder.append(UI.getCurrent().getUIId()).append(PATH_SEPARATOR);
            builder.append(id).append(PATH_SEPARATOR);
            builder.append(
                    URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    private static Logger getLog() {
        return Logger.getLogger(StreamResourceRequestHandler.class.getName());
    }

}
