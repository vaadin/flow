/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.testutil;

import java.util.Map;

import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.Response;

import com.vaadin.testbench.TestBenchDriverProxy;

/**
 * Phantom JS driver which waits for Vaadin to finish after every command and
 * not just after find commands.
 * <p>
 * Workaround for https://dev.vaadin.com/ticket/19753
 *
 * @author Vaadin Ltd
 */
public class FixedPhantomJSDriver extends PhantomJSDriver {

    private TestBenchDriverProxy testBenchDriverProxy;

    /**
     * Create a new driver instance.
     *
     * @param cap
     *            the desired capabilities
     */
    public FixedPhantomJSDriver(DesiredCapabilities cap) {
        super(cap);
    }

    @Override
    protected Response execute(String driverCommand,
            Map<String, ?> parameters) {
        try {
            return super.execute(driverCommand, parameters);
        } finally {
            if (testBenchDriverProxy != null) {
                // Wait after all commands but avoid looping
                Object scriptParam = parameters.get("script");
                if (!"quit".equals(driverCommand)
                        && !"executeScript".equals(driverCommand)
                        && !(scriptParam instanceof String
                                && ((String) scriptParam)
                                        .contains("window.vaadin"))) {
                    testBenchDriverProxy.waitForVaadin();
                }
            }
        }
    }

    /**
     * Sets the TestBench proxy.
     *
     * @param testBenchDriverProxy
     *            the TestBench proxy
     */
    public void setTestBenchDriverProxy(
            TestBenchDriverProxy testBenchDriverProxy) {
        this.testBenchDriverProxy = testBenchDriverProxy;

    }

}
