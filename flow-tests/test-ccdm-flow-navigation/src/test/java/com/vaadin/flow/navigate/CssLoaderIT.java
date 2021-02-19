/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.navigate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CssLoaderIT extends ChromeBrowserTest {
    @Test
    public void aboutView_should_haveColorDefinedInGloablCssFile() {
        open();
        String realColor = executeScript(
            "return window.getComputedStyle("
                + "document.getElementsByTagName('about-view')[0], null)"
                + ".getPropertyValue('color')"
        ).toString();
        // the real color of salmon that is defined in the css file
        String expectedRealColor = "rgb(250, 128, 114)";
        Assert.assertEquals(expectedRealColor, realColor);
    }

    @Override
    protected String getTestPath() {
        return "/context-path/about";
    }
}
