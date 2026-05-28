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
package com.vaadin.flow.micrometer.spring.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Drives a real plain-Spring Vaadin Flow page in Chrome and asserts the
 * Spring-managed {@code MeterRegistry} (provided through
 * {@code VaadinMicrometerSpringConfiguration}) saw session/UI/request activity.
 */
public class SpringMicrometerIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void viewLoadDrivesSessionAndUiMetrics() throws IOException {
        open();

        WebElement greeting = findElement(By.id("greeting"));
        Assert.assertEquals("Hello micrometer spring", greeting.getText());

        String metrics = fetchMetrics();

        Assert.assertTrue(
                "expected vaadin.sessions.created counter > 0, got:\n"
                        + metrics,
                meterValue(metrics, "vaadin.sessions.created", "count") >= 1.0);
        Assert.assertTrue(
                "expected vaadin.ui.created counter > 0, got:\n" + metrics,
                meterValue(metrics, "vaadin.ui.created", "count") >= 1.0);
        Assert.assertTrue(
                "expected vaadin.sessions.active gauge > 0, got:\n" + metrics,
                meterValue(metrics, "vaadin.sessions.active", "value") >= 1.0);
        Assert.assertTrue(
                "expected at least one vaadin.request.duration sample, got:\n"
                        + metrics,
                metrics.contains("vaadin.request.duration"));
    }

    private String fetchMetrics() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI
                .create(getRootURL() + "/metrics").toURL().openConnection();
        conn.setRequestMethod("GET");
        Assert.assertEquals(200, conn.getResponseCode());
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private static double meterValue(String metricsBody, String meterName,
            String field) {
        Pattern pattern = Pattern.compile(
                "^" + Pattern.quote(meterName) + "(?:\\s.*)?\\s"
                        + Pattern.quote(field) + "=([0-9]+(?:\\.[0-9]+)?)",
                Pattern.MULTILINE);
        Matcher m = pattern.matcher(metricsBody);
        return m.find() ? Double.parseDouble(m.group(1)) : -1.0;
    }
}
