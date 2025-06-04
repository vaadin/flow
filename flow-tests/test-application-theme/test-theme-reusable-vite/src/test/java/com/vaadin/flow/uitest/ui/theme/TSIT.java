
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
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TSIT extends ChromeBrowserTest {

    @Test
    public void lumoBadgeIsRenderedCorrectly() {
        open();
        checkLogsForErrors();

        DivElement badge = $("ts-component").first().$(DivElement.class)
                .withAttribute("theme", "badge").first();
        String badgeBackgroundColor = badge.getCssValue("backgroundColor");
        Assert.assertEquals("rgba(51, 139, 255, 0.13)", badgeBackgroundColor);
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "path/");
    }

}
