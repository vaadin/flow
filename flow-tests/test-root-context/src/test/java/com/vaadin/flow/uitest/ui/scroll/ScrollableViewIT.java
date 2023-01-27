/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class ScrollableViewIT extends ChromeBrowserTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return ScrollableView.class;
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    @Test
    public void scrollIntoView() {
        Assert.assertTrue(getScrollY() != 0);
        scrollBy(0, -getScrollY());
        Assert.assertTrue(getScrollY() == 0);
        findElement(By.id("button")).click();
        Assert.assertTrue(getScrollY() > 0);
    }

}
