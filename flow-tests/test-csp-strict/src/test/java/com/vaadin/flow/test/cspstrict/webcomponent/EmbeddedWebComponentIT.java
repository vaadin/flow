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

import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.test.cspstrict.AbstractCspStrictIT;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * A server-side component embedded as a web component in a page that Vaadin
 * does not serve and that sends a strict policy of its own.
 */
public class EmbeddedWebComponentIT extends AbstractCspStrictIT {

    private static final Pattern NONCE = Pattern
            .compile("'nonce-([A-Za-z0-9+/=]+)'");

    @Override
    protected String getTestPath() {
        return EmbeddedHostPage.PATH;
    }

    @BrowserTest
    public void hostPage_sendsThePolicyOfAVaadinPage() throws Exception {
        HttpResponse<String> hostPage = fetchBootstrapPage();
        String policy = getPolicy(hostPage);
        Matcher nonce = NONCE.matcher(policy);
        Assertions.assertTrue(nonce.find(),
                "No nonce in the policy of the host page: " + policy);

        // The policy of the host page is written by hand: it has to stay the
        // one Vaadin sends for its own pages, apart from the nonce
        String vaadinPolicy = getPolicy(fetch("/policy"));
        Assertions.assertEquals(NONCE.matcher(vaadinPolicy).replaceAll("nonce"),
                NONCE.matcher(policy).replaceAll("nonce"));

        Elements scripts = Jsoup.parse(hostPage.body()).select("script");
        Assertions.assertEquals(3, scripts.size());
        for (Element script : scripts) {
            Assertions.assertEquals(nonce.group(1), script.attr("nonce"),
                    "Script without the nonce of the response: " + script);
        }
    }

    @BrowserTest
    public void webComponent_rendersAndSyncsUnderThePolicyOfTheHostPage() {
        open();

        // The attribute of the host page reaches the server-side component
        waitUntil(driver -> "Hello, host".equals(getGreeting()));

        // A property set by the host page reaches it too
        ((JavascriptExecutor) getDriver()).executeScript(
                "document.getElementById('embedded').name = 'strict'");
        waitUntil(driver -> "Hello, strict".equals(getGreeting()));

        // A click is handled on the server, which updates the component and
        // fires an event on the web component for the host page
        inShadowRoot("count").click();
        waitUntil(driver -> "1".equals(inShadowRoot("clicks").getText()));
        waitUntil(driver -> "1"
                .equals(findElement(By.id("host-count")).getText()));

        Assertions.assertEquals("Hello, strict", getGreeting());
        assertNoCspViolations();
    }

    private static String getPolicy(HttpResponse<String> response) {
        return response.headers().firstValue("Content-Security-Policy")
                .orElseThrow(() -> new AssertionError(
                        "No Content-Security-Policy header"));
    }

    private String getGreeting() {
        return inShadowRoot("greeting").getText();
    }

    private TestBenchElement inShadowRoot(String id) {
        return $(TestBenchElement.class).id("embedded")
                .$(TestBenchElement.class).id(id);
    }
}
