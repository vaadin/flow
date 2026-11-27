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
package com.vaadin.flow.test.cspstrict.springsecurity;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks which Content Security Policy a response carries when both Vaadin, in
 * the {@code strict} mode, and Spring Security are set to send one, and that
 * the other headers of Spring Security reach the bootstrap page.
 */
public class SpringSecurityPolicyIT {

    private static final String HEADER = "Content-Security-Policy";

    private static final Pattern BUNDLE_SCRIPT = Pattern
            .compile("VAADIN/build/[^\"']+\\.js");

    private static final Pattern DOWNLOAD_URL = Pattern
            .compile("VAADIN/dynamic/resource/[^\"\\\\]+");

    /**
     * Failsafe passes the {@code server.port} property of the pom as the
     * {@code serverPort} system property. The fallback is for running a test
     * from the IDE against an app started by Maven.
     */
    private static final String ROOT_URL = "http://localhost:"
            + System.getProperty("serverPort", "8891");

    @Test
    public void bootstrapPage_carriesThePolicyOfSpringSecurityWithTheNonceOfItsScripts()
            throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = fetch(client, "/");

            String nonce = assertPolicyOfSpringSecurity(response);
            Matcher scripts = Pattern.compile("<script[^>]*>")
                    .matcher(response.body());
            int count = 0;
            while (scripts.find()) {
                count++;
                assertTrue(scripts.group().contains("nonce=\"" + nonce + "\""),
                        "Every script should have the nonce of the policy: "
                                + scripts.group());
            }
            assertTrue(count > 0, "No script in: " + response.body());
        }
    }

    @Test
    public void bootstrapPageWithoutPolicyOfSpringSecurity_carriesThePolicyOfVaadin()
            throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = fetch(client,
                    "/" + SecurityConfig.PATH_WITHOUT_SPRING_SECURITY_POLICY);

            assertEquals(200, response.statusCode());
            Matcher script = Pattern.compile("<script[^>]* nonce=\"([^\"]+)\"")
                    .matcher(response.body());
            assertTrue(script.find(), "No script in: " + response.body());
            // Spring Security creates the nonce but sends no policy for this
            // path, which must not leave the page without one
            assertEquals(List.of("default-src 'self'; script-src 'nonce-"
                    + script.group(1) + "' 'strict-dynamic'; "
                    + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                    + "font-src 'self' data:; connect-src 'self' data:; "
                    + "worker-src 'self'; object-src 'none'; base-uri 'self'"),
                    response.headers().allValues(HEADER));
        }
    }

    @Test
    public void bootstrapPage_carriesTheOtherHeadersOfSpringSecurity()
            throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpHeaders headers = fetch(client, "/").headers();

            // Spring Security writes these only when the response is
            // committed, which the forward to the Vaadin servlet must let it
            // see
            assertEquals(List.of("nosniff"),
                    headers.allValues("X-Content-Type-Options"));
            assertEquals(List.of("DENY"), headers.allValues("X-Frame-Options"));
        }
    }

    @Test
    public void bundleScript_carriesThePolicyOfSpringSecurity()
            throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String script = find(BUNDLE_SCRIPT, fetch(client, "/").body());

            HttpResponse<String> response = fetch(client, "/" + script);

            assertEquals(200, response.statusCode());
            assertPolicyOfSpringSecurity(response);
        }
    }

    @Test
    public void download_carriesThePolicyOfSpringSecurity() throws Exception {
        // The download URL belongs to a UI, which belongs to a session. The
        // response to the request that creates the UI has the URL
        try (HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager()).build()) {
            String url = find(DOWNLOAD_URL,
                    fetch(client, "/?v-r=init&location=").body());

            HttpResponse<String> response = fetch(client, "/" + url);

            assertEquals(200, response.statusCode());
            assertEquals(DownloadLink.CONTENT_TYPE,
                    response.headers().firstValue("Content-Type").orElse(null));
            assertPolicyOfSpringSecurity(response);
        }
    }

    @Test
    public void responseOfSpringSecurity_carriesThePolicyOfSpringSecurity()
            throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // Not a public view, and no login view to redirect to: Spring
            // Security answers the request itself
            HttpResponse<String> response = fetch(client, "/needs-login");

            assertEquals(403, response.statusCode());
            assertPolicyOfSpringSecurity(response);
        }
    }

    /**
     * Checks that a response has one policy, which is the one configured in
     * Spring Security with a nonce in place of the placeholder.
     *
     * @return the nonce
     */
    private static String assertPolicyOfSpringSecurity(
            HttpResponse<String> response) {
        List<String> policies = response.headers().allValues(HEADER);
        assertEquals(1, policies.size(),
                "The response should have one policy: " + policies);
        String[] parts = SecurityConfig.SPRING_SECURITY_POLICY
                .split(Pattern.quote("{nonce}"));
        Matcher matcher = Pattern.compile(Pattern.quote(parts[0])
                + "([A-Za-z0-9+/]{22})" + Pattern.quote(parts[1]))
                .matcher(policies.get(0));
        assertTrue(matcher.matches(),
                "The policy should be the one of Spring Security with a nonce: "
                        + policies.get(0));
        return matcher.group(1);
    }

    private static String find(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find(), "No " + pattern + " in: " + body);
        return matcher.group();
    }

    private static HttpResponse<String> fetch(HttpClient client, String path)
            throws IOException, InterruptedException {
        return client.send(
                HttpRequest.newBuilder(URI.create(ROOT_URL + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
