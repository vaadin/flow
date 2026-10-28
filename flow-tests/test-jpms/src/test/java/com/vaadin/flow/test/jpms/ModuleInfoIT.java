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
package com.vaadin.flow.test.jpms;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ModuleInfoIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @BeforeClass
    public static void waitForServer() throws Exception {
        // The application is started asynchronously before the tests
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + SERVER_PORT))
                .build();
        Instant deadline = Instant.now().plus(Duration.ofMinutes(1));
        while (true) {
            try {
                client.send(request, HttpResponse.BodyHandlers.discarding());
                return;
            } catch (IOException e) {
                if (Instant.now().isAfter(deadline)) {
                    throw e;
                }
                Thread.sleep(500);
            }
        }
    }

    @Test
    public void applicationAndFlowRunAsExplicitModules() {
        open();

        Assert.assertEquals("com.vaadin.flow.test.jpms explicit",
                $("span").id("app-module").getText());
        Assert.assertEquals("com.vaadin.flow.server explicit",
                $("span").id("flow-module").getText());
        Assert.assertEquals("com.vaadin.flow.component.html explicit",
                $("span").id("components-module").getText());

        $("button").id("button").click();
        Assert.assertEquals("Clicked", $("div").id("clicked").getText());
    }

    @Test
    public void pushFromBackgroundThread() {
        open();

        $("button").id("push-button").click();
        waitUntil(driver -> "Pushed"
                .equals($("div").id("pushed").getText()));
    }
}
