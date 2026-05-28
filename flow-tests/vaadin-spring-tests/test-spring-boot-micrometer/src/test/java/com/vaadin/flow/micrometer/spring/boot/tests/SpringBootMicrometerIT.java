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
package com.vaadin.flow.micrometer.spring.boot.tests;

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
 * Drives a Spring Boot + vaadin-micrometer-spring-boot app in Chrome and
 * asserts the auto-configured binders moved the Vaadin meters, scraping the
 * result via the actuator's Prometheus endpoint.
 */
public class SpringBootMicrometerIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void viewLoadDrivesMetricsExposedViaActuator() throws IOException {
        open();

        WebElement greeting = findElement(By.id("greeting"));
        Assert.assertEquals("Hello micrometer boot", greeting.getText());

        String metrics = fetchPrometheus();

        // Prometheus / OpenMetrics treats the `_created` suffix as a special
        // timestamp marker and drops it from counter names, so the Micrometer
        // counter `vaadin.sessions.created` is emitted as
        // `vaadin_sessions_total`
        // (and `vaadin.ui.created` as `vaadin_ui_total`).
        Assert.assertTrue(
                "expected vaadin_sessions_total in prometheus output, got:\n"
                        + metrics,
                meterValue(metrics, "vaadin_sessions_total") >= 1.0);
        Assert.assertTrue(
                "expected vaadin_ui_total in prometheus output, got:\n"
                        + metrics,
                meterValue(metrics, "vaadin_ui_total") >= 1.0);
        Assert.assertTrue(
                "expected vaadin_sessions_active in prometheus output, got:\n"
                        + metrics,
                meterValue(metrics, "vaadin_sessions_active") >= 1.0);
        Assert.assertTrue(
                "expected vaadin_request_duration_seconds metric, got:\n"
                        + metrics,
                metrics.contains("vaadin_request_duration_seconds"));
    }

    private String fetchPrometheus() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI
                .create(getRootURL() + "/actuator/prometheus").toURL()
                .openConnection();
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

    /**
     * Finds the numeric value of the first sample line whose metric name
     * matches {@code meterName}. Lines starting with {@code #} (Prometheus
     * comments) and lines with tags are accepted. Returns {@code -1.0} if no
     * match.
     */
    private static double meterValue(String prometheusBody, String meterName) {
        Pattern pattern = Pattern.compile(
                "^" + Pattern.quote(meterName) + "(?:\\{[^}]*\\})?\\s+"
                        + "([0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?)",
                Pattern.MULTILINE);
        Matcher m = pattern.matcher(prometheusBody);
        return m.find() ? Double.parseDouble(m.group(1)) : -1.0;
    }
}
