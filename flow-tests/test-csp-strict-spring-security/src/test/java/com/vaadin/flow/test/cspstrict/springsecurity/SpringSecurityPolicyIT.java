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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks which Content Security Policy a response carries when both Vaadin, in
 * the {@code strict} mode, and Spring Security are set to send one.
 */
public class SpringSecurityPolicyIT {

    private static final String HEADER = "Content-Security-Policy";

    /**
     * Failsafe passes the {@code server.port} property of the pom as the
     * {@code serverPort} system property. The fallback is for running a test
     * from the IDE against an app started by Maven.
     */
    private static final String ROOT_URL = "http://localhost:"
            + System.getProperty("serverPort", "8891");

    @Test
    public void bootstrapPage_carriesOnlyThePolicyOfVaadin() throws Exception {
        List<String> policies = fetch("/").headers().allValues(HEADER);

        assertEquals(1, policies.size(),
                "The bootstrap page should have one policy: " + policies);
        String policy = policies.get(0);
        assertTrue(policy.contains("script-src 'nonce-"),
                "The policy should be the nonce-based one of Vaadin: "
                        + policy);
        assertFalse(policy.contains("form-action"),
                "The policy of Spring Security should not be merged in: "
                        + policy);
    }

    @Test
    public void bootstrapPage_carriesTheDirectiveAddedThroughTheListener()
            throws Exception {
        String policy = fetch("/").headers().firstValue(HEADER).orElseThrow(
                () -> new AssertionError("No " + HEADER + " header"));

        String directive = PolicyAdjuster.DIRECTIVE + " "
                + PolicyAdjuster.SOURCE;
        assertTrue(List.of(policy.split("; ")).contains(directive),
                "No " + directive + " in the policy: " + policy);
    }

    @Test
    public void responseOfSpringSecurity_carriesThePolicyOfSpringSecurity()
            throws Exception {
        // Not a public view, and no login view to redirect to: Spring
        // Security answers the request itself
        HttpResponse<String> response = fetch("/needs-login");

        assertEquals(403, response.statusCode());
        assertEquals(List.of(SecurityConfig.SPRING_SECURITY_POLICY),
                response.headers().allValues(HEADER));
    }

    private static HttpResponse<String> fetch(String path)
            throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(URI.create(ROOT_URL + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
