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
package com.vaadin.flow.uitest.ui.notheme;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.Assert;
import org.junit.Test;

public class NoThemeComponentIT extends ChromeBrowserTest {

    @Test
    public void themeIsNotApplied() {
        open();

        TestBenchElement link = $("a").first();
        String text = link.getText();
        Assert.assertEquals("Hello notheme", text);
        String color = link.getCssValue("color");
        Assert.assertEquals(
                "Unexpected color for a link. "
                        + "@NoTheme should not theme a link anyhow.",
                "rgba(0, 0, 0, 1)", color);
    }
}
