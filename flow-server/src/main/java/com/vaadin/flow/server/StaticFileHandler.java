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
package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serializable;

/**
 * Handler for static files. The handler should be responsible in identifying
 * and serving the static files, based on the servlet request.
 * 
 * @author Vaadin Ltd.
 * @since 1.3
 *
 * @see StaticFileServer
 */
public interface StaticFileHandler extends Serializable {

    /**
     * Checks if a static resource can be found for the requested path.
     *
     * @param request
     *            the request to check
     * @return <code>true</code> if a static resource exists and can be sent as
     *         a response to this request, <code>false</code> otherwise
     */
    boolean isStaticResourceRequest(HttpServletRequest request);

    /**
     * Serves a static resource for the requested path if a resource can be
     * found.
     *
     * @param request
     *            the request object to read from
     * @param response
     *            the response object to write to
     * @return <code>true</code> if a file was served and the request has been
     *         handled, <code>false</code> otherwise.
     * @throws IOException
     *             if the underlying servlet container reports an exception
     */
    boolean serveStaticResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

}
