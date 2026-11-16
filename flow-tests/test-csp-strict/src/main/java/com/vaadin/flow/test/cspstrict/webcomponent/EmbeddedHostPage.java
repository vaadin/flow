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
package com.vaadin.flow.test.cspstrict.webcomponent;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.vaadin.flow.test.cspstrict.CspViolationCollector;

/**
 * Serves the host page of the embedded web component: a plain HTML page that is
 * not a Vaadin page, with a strict policy of its own.
 * <p>
 * Vaadin does not serve the page, so it can neither send the policy nor put a
 * nonce on the scripts: the page does both itself, as the application that
 * embeds the web component would. The policy is the one Vaadin sends for its
 * own pages in {@code strict} mode, with a new nonce for every response. The
 * only scripts of the page are the violation collector, the script that loads
 * the web component and the listener for its event.
 */
@Component
public class EmbeddedHostPage implements Filter {

    /**
     * Path of the host page.
     */
    public static final String PATH = "/embedded-host";

    private static final String PAGE = """
            <!doctype html>
            <html>
            <head>
            <meta charset="utf-8">
            <script nonce="%1$s">
            %2$s</script>
            <script nonce="%1$s" type="module"
                src="/web-component/%3$s.js"></script>
            </head>
            <body>
            <%3$s id="embedded" name="host"></%3$s>
            <span id="host-count"></span>
            <script nonce="%1$s">
            document.getElementById('embedded').addEventListener('counted',
              function (event) {
                document.getElementById('host-count').textContent =
                    event.detail.count;
              });
            </script>
            </body>
            </html>
            """;

    private final SecureRandom random = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!PATH.equals(((HttpServletRequest) request).getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String nonce = Base64.getEncoder().encodeToString(bytes);

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " + "script-src 'nonce-" + nonce
                        + "' 'strict-dynamic'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; font-src 'self' data:; "
                        + "connect-src 'self'; worker-src 'self'; "
                        + "object-src 'none'; base-uri 'self'");
        httpResponse.setContentType("text/html");
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.getWriter().write(PAGE.formatted(nonce,
                CspViolationCollector.SCRIPT, EmbeddedExporter.TAG));
    }
}
