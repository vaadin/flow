/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceWriter;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 * 
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRequestHandler implements RequestHandler {

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        StreamResourceWriter writer = null;

        if (request.getPathInfo() == null) {
            return false;
        }
        String pathInfo = request.getPathInfo();
        // remove leading '/'
        pathInfo = pathInfo.substring(1);

        session.lock();
        try {
            int index = pathInfo.lastIndexOf('/');
            String path = index >= 0 ? pathInfo.substring(0, index + 1) : "";
            String name = pathInfo.substring(path.length());
            // path info returns decoded name but space ' ' remains encoded '+'
            name = name.replace('+', ' ');
            Optional<StreamResource> resource = session.getResourceRegistry()
                    .getResource(path, name);
            if (!resource.isPresent()) {
                return false;
            }

            response.setContentType(resource.get().getContentType());
            response.setCacheTime(resource.get().getCacheTime());
            writer = resource.get().getWriter();
            if (writer == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }
        } finally {
            session.unlock();
        }
        OutputStream outputStream = response.getOutputStream();
        try {
            writer.accept(outputStream, session);
        } finally {
            outputStream.close();
        }
        return true;
    }

}
