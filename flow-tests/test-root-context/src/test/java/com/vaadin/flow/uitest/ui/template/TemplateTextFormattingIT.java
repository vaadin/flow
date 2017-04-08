/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.PhantomJSTest;
import com.vaadin.testbench.By;

public class TemplateTextFormattingIT extends PhantomJSTest {

    private Set<String> doNotMatch = new HashSet<>();
    {
        doNotMatch.add("Double SemiLarge"); // 1e+20 vs 100000000000000000000
        doNotMatch.add("Double SemiSmall"); // -1e+20 vs -100000000000000000000
        doNotMatch.add("Double Min"); // 4.9e-324 vs 5e-324
    }

    @Test
    public void doubleValuesMatch() {
        open("prerender=only");
        List<WebElement> rows = findElement(By.tagName("table"))
                .findElements(By.tagName("tr"));

        Map<String, String> prerenderValues = new HashMap<>();
        rows.forEach(tr -> {
            List<WebElement> tds = tr.findElements(By.tagName("td"));
            if (tds.size() == 0) {
                // Ignore header with th instead of td
                return;
            }
            String name = tds.get(0).getText();
            String value = tds.get(1).getText();
            prerenderValues.put(name, value);
        });

        open("prerender=no");
        rows = findElement(By.tagName("table")).findElements(By.tagName("tr"));

        rows.forEach(tr -> {
            List<WebElement> tds = tr.findElements(By.tagName("td"));
            if (tds.size() == 0) {
                // Ignore header with th instead of td
                return;
            }
            String name = tds.get(0).getText();
            String value = tds.get(1).getText();
            if (!doNotMatch.contains(name)) {
                Assert.assertEquals("Value for '" + name + "' does not match",
                        prerenderValues.get(name), value);
            }
        });

    }

}
