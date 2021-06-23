/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class SessionValueIT extends ChromeBrowserTest {

        @Test
        public void sessionValuePreservedOnRefresh() throws InterruptedException {
                open();
                TestBenchElement div = $("div").id("customAttribute");
                String customAttribute = div.getText().replace("The custom value in the session is: ", "");

                // trigger jetty reload
                findElement(By.id(WebpackDevServerPortView.TRIGGER_RELOAD_ID)).click();

                waitForElementPresent(By.id("customAttribute"));
                div = $("div").id("customAttribute");
                String customAttributeAfterReload = div.getText().replace("The custom value in the session is: ", "");
                Assert.assertEquals(customAttribute, customAttributeAfterReload);

        }
}
