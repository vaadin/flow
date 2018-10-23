/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osgi.service.http.HttpContext;

public final class StaticResourceServlet extends HttpServlet {

    private String path;

    private HttpContext context;

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        String resName = (pathInfo == null ? path : path + pathInfo);

        final URL resourceUrl = context.getResource(resName);

        if (resourceUrl == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            handle(request, response, resourceUrl, resName);
        }
    }

    void setPath(String path) {
        this.path = path;
    }

    void setContext(HttpContext context) {
        this.context = context;
    }

    private void handle(HttpServletRequest request,
            HttpServletResponse response, final URL url, String resName)
            throws IOException {
        String mimeType = getServletContext().getMimeType(resName);
        if (mimeType != null) {
            response.setContentType(mimeType);
        }

        URLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            connection = url.openConnection();

            int len = getContentLength(connection);
            if (len >= 0) {
                response.setContentLength(len);
            }

            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();
            IOUtils.copy(inputStream, outputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private int getContentLength(final URLConnection conn) {
        int length = conn.getContentLength();
        if (length < 0) {
            String path = conn.getURL().getPath();
            if (path != null) {
                File file = new File(path);
                length = (int) file.length();
            }
        }
        return length;
    }

}
