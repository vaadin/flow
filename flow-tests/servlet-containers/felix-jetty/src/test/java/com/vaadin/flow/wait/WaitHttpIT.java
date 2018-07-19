/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.wait;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WaitHttpIT extends ChromeBrowserTest {

    @Test
    public void waitForHttp() {
        getDriver().manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
        waitViewUrl(5);
    }

    private void waitViewUrl(int count) {
        if (count == 0) {
            throw new IllegalStateException(
                    "URL '" + getRootURL() + "/view' is not avialable");
        }
        try {
            getDriver().get(getRootURL() + "/view");
        } catch (IllegalStateException exception) {
            waitViewUrl(count - 1);
        }
    }

}
