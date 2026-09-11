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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.ee10.proxy.ProxyServlet;
import org.eclipse.jetty.http.HttpField;

public class PathRewritingProxyServlet extends ProxyServlet.Transparent {

    private String prefix;
    private String proxyTo;

    @Override
    public void init(ServletConfig config) throws ServletException {
        proxyTo = config.getInitParameter("proxyTo");
        prefix = config.getInitParameter("prefix");
        super.init(config);
    }

    @Override
    protected HttpField filterServerResponseHeader(
            HttpServletRequest clientRequest, Response serverResponse,
            HttpField field) {
        String headerName = field.getName();
        String headerValue = field.getValue();
        if (headerName.toLowerCase(Locale.ENGLISH).equals("set-cookie")) {
            // Set-Cookie: JSESSIONID=07E35F87D336463E597B5B0D32744660; Path=/;
            // HttpOnly
            return replaceValue(field,
                    headerValue.replace("Path=/", "Path=" + prefix));
        } else if (headerName.equals("Location")) {
            // Location: http://localhost:8888/my/login/page
            if ((headerValue.startsWith("http://")
                    || headerValue.startsWith("https://"))
                    && !headerValue.startsWith(proxyTo)) {
                // External location
                return replaceValue(field, headerValue);
            }

            try {
                URL publicURL = URI
                        .create(clientRequest.getRequestURL().toString())
                        .toURL();
                String hostAndBasePath = publicURL.getProtocol() + "://"
                        + publicURL.getHost() + ":" + publicURL.getPort()
                        + prefix + "/";

                if (headerValue.startsWith(proxyTo)) {
                    return replaceValue(field,
                            headerValue.replace(proxyTo, hostAndBasePath));
                } else {
                    // Location: /foo/bar
                    return replaceValue(field, prefix + headerValue);
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Unable to rewrite header "
                        + headerName + ": " + headerValue);
            }

        }
        return super.filterServerResponseHeader(clientRequest, serverResponse,
                field);
    }

    private HttpField replaceValue(HttpField field, String value) {
        return new HttpField(field.getHeader(), field.getName(), value);
    }
}
