/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for VAADIN_USAGE_STATS_ENABLED environment variable affecting
 * Constants.DEFAULT_DEVMODE_STATS.
 */
public class ConstantsUsageStatsEnvTest {

    private String runIsolated(Boolean setEnv, String value)
            throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + java.io.File.separator + "bin"
                + java.io.File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath,
                "com.vaadin.flow.server.PrintDefaultDevModeStatsMain");

        Map<String, String> env = builder.environment();
        if (setEnv == null || !setEnv) {
            // Ensure the environment variable is not present
            env.remove(Constants.VAADIN_USAGE_STATS_ENABLED);
        } else {
            env.put(Constants.VAADIN_USAGE_STATS_ENABLED, value);
        }

        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new AssertionError("Child JVM exited with code " + exit
                        + "; output: " + line);
            }
            return line == null ? "" : line.trim();
        }
    }

    @Test
    public void whenEnvNotSet_statsEnabledByDefault() throws Exception {
        String out = runIsolated(false, null);
        Assert.assertEquals("true", out);
    }

    @Test
    public void whenEnvFalse_statsDisabled() throws Exception {
        String out = runIsolated(true, "false");
        Assert.assertEquals("false", out);
    }

    @Test
    public void whenEnvFALSE_statsDisabled() throws Exception {
        String out = runIsolated(true, "FALSE");
        Assert.assertEquals("false", out);
    }

    @Test
    public void whenEnvTrue_statsEnabled() throws Exception {
        String out = runIsolated(true, "true");
        Assert.assertEquals("true", out);
    }

    @Test
    public void whenEnvRandom_statsEnabled() throws Exception {
        String out = runIsolated(true, "random-value");
        Assert.assertEquals("true", out);
    }
}
