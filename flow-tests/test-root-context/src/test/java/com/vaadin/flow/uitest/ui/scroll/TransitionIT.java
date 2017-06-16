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

package com.vaadin.flow.uitest.ui.scroll;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Objects;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;


/**
 * @author Vaadin Ltd.
 */
public class TransitionIT extends ChromeBrowserTest {

    @Test
    public void scrollPositionIsSaved() {
        open();

        final String initialPageUrl = driver.getCurrentUrl();
        final int xScrollAmount = 0;
        final int yScrollAmount = 400;

        scrollBy(xScrollAmount, yScrollAmount);
        ensureThatPageRemainsScrolled(xScrollAmount, yScrollAmount);

        // Cannot use findElement(By.id(TransitionView.URL_ID)).click() because it changes scroll position
        executeScript(String.format("document.getElementById('%s').click();", TransitionView.URL_ID));

        while (true) {
            if (Objects.equals(initialPageUrl, driver.getCurrentUrl())) {
                ensureThatPageRemainsScrolled(xScrollAmount, yScrollAmount);
            } else {
                ensureThatNewPageIsNotScrolled();
                break;
            }
        }

        findElement(By.id(LongToOpenView.BACK_BUTTON_ID)).click();

        assertThat("Did not return back on initial page", driver.getCurrentUrl(), is(initialPageUrl));
        ensureThatPageRemainsScrolled(xScrollAmount, yScrollAmount);
    }

    private void ensureThatNewPageIsNotScrolled() {
        ensureThatPageRemainsScrolled(0, 0);
    }

    private void ensureThatPageRemainsScrolled(int xScrollAmount, int yScrollAmount) {
        assertThat("Unexpected x scroll position", getScrollX(), is(xScrollAmount));
        assertThat("Unexpected y scroll position", getScrollY(), is(yScrollAmount));
    }
}
