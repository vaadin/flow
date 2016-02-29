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

import org.junit.Assert;
import org.junit.Test;

/**
 * A testbench test for PhantomJS that verifies the used PhantomJS version.
 */
public class PhantomJSVersionIT extends PhantomJSTest {

    /**
     * Verifies that the system PhantomJS version matches the expected one.
     */
    @Test
    public void checkPhantomJsVersion() {
        String userAgent = (String) executeScript(
                "return navigator.userAgent;");
        // Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/538.1 (KHTML,
        // like Gecko) PhantomJS/2.1.1 Safari/538.1
        Assert.assertTrue(userAgent.contains(" PhantomJS/2.1."));
    }
}
