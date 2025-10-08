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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.component.Direction;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DirectionChangeIT extends ChromeBrowserTest {

    private static final String RETURN_DIR_SCRIPT = "return document.dir;";

    @Test
    public void testDirection_changingDirection_updatesDocument() {
        open();

        // the value of document.dir is "" initially
        verifyDirection(null);

        clickElementWithJs("rtl-button");
        verifyDirection(Direction.RIGHT_TO_LEFT);

        clickElementWithJs("ltr-button");
        verifyDirection(Direction.LEFT_TO_RIGHT);
    }

    @Test
    public void testDirection_initialPageDirection_setCorrectly() {
        open("rtl");

        // due to #8028 / 8029 need to wait for JS execution to complete
        waitUntil(webDriver -> ((JavascriptExecutor) driver)
                .executeScript(RETURN_DIR_SCRIPT), 2);

        verifyDirection(Direction.RIGHT_TO_LEFT);

        clickElementWithJs("ltr-button");

        verifyDirection(Direction.LEFT_TO_RIGHT);
    }

    private void verifyDirection(Direction direction) {
        String dir = getDirection();
        Assert.assertEquals(direction == null ? "" : direction.getClientName(),
                dir);
    }

    private String getDirection() {
        return (String) executeScript(RETURN_DIR_SCRIPT);
    }
}
