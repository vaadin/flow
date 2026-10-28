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
package com.vaadin.flow.test.cspstrict.csp;

import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolicyIT extends AbstractCspStrictIT {

    private static final String HEADER = "Content-Security-Policy";

    private static final Pattern NONCE = Pattern
            .compile("'nonce-([A-Za-z0-9+/=]+)'");

    @Override
    protected String getTestPath() {
        return "/policy";
    }

    @BrowserTest
    public void bootstrapPage_hasStrictPolicyAndNonceOnEveryScript()
            throws Exception {
        HttpResponse<String> response = fetchBootstrapPage();

        String policy = response.headers().firstValue(HEADER).orElseThrow(
                () -> new AssertionError("No " + HEADER + " header"));
        String nonce = findNonce(policy);
        String scriptSrc = findDirective(policy, "script-src");
        assertTrue(scriptSrc.contains("'strict-dynamic'"),
                "script-src should trust the scripts the nonced scripts load: "
                        + scriptSrc);
        assertFalse(scriptSrc.contains("'unsafe-"),
                "script-src should allow neither eval nor inline scripts: "
                        + scriptSrc);
        Assertions.assertEquals("'none'", findDirective(policy, "object-src"));

        Document document = Jsoup.parse(response.body());
        for (Element script : document.select("script")) {
            Assertions.assertEquals(nonce, script.attr("nonce"),
                    "Script without the nonce of the response: " + script);
        }
        assertTrue(
                document.head().select("script").first().data()
                        .contains("securitypolicyviolation"),
                "The violation collector should be the first script of the head");
    }

    @BrowserTest
    public void bootstrapPage_newNonceForEveryResponse() throws Exception {
        String first = findNonce(fetchBootstrapPage().headers()
                .firstValue(HEADER).orElseThrow());
        String second = findNonce(fetchBootstrapPage().headers()
                .firstValue(HEADER).orElseThrow());
        assertNotEquals(first, second);
    }

    @BrowserTest
    @Disabled(AbstractCspStrictIT.NO_UI_IN_STRICT_MODE)
    public void view_rendersUnderThePolicy() {
        open();

        Assertions.assertEquals("Rendered under the policy",
                $(SpanElement.class).id("content").getText());
        assertNoCspViolations();
    }

    private static String findNonce(String policy) {
        Matcher matcher = NONCE.matcher(policy);
        assertTrue(matcher.find(), "No nonce in the policy: " + policy);
        return matcher.group(1);
    }

    private static String findDirective(String policy, String directive) {
        for (String entry : policy.split(";")) {
            String trimmed = entry.trim();
            if (trimmed.startsWith(directive + " ")) {
                return trimmed.substring(directive.length() + 1);
            }
        }
        throw new AssertionError(
                "No " + directive + " directive in the policy: " + policy);
    }
}
