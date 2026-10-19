/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClearAndReplaceChildrenIT extends ChromeBrowserTest {

    /**
     * A container that is momentarily empty makes the browser clamp the scroll
     * offset of the scrollable element around it to the collapsed scroll range,
     * and the offset is not restored when the contents come back. Whether the
     * offset visibly breaks depends on how eagerly the browser clamps, so this
     * asserts the invariant that the fix provides rather than the symptom: the
     * DOM mutations replacing the contents never leave the container empty.
     */
    @Test
    public void replaceContent_containerIsNeverEmpty() {
        open();
        waitForElementPresent(By.id("scroller"));

        getCommandExecutor().executeScript(
                """
                        const content = document.getElementById('scroller').firstElementChild;
                        content.firstElementChild.dataset.replaced = 'true';
                        let childCount = content.childElementCount;
                        window.minChildCount = childCount;
                        window.maxChildCount = childCount;
                        new MutationObserver((records) => {
                          for (const record of records) {
                            childCount -= record.removedNodes.length;
                            childCount += record.addedNodes.length;
                            window.minChildCount = Math.min(window.minChildCount, childCount);
                            window.maxChildCount = Math.max(window.maxChildCount, childCount);
                          }
                        }).observe(content, { childList: true });
                        """);

        findElement(By.id("replace")).click();

        waitUntil(driver -> getCommandExecutor().executeScript(
                """
                        const content = document.getElementById('scroller').firstElementChild;
                        return content.childElementCount === 1
                            && !content.firstElementChild.dataset.replaced;
                        """));

        Long minChildCount = (Long) getCommandExecutor()
                .executeScript("return window.minChildCount;");
        Long maxChildCount = (Long) getCommandExecutor()
                .executeScript("return window.maxChildCount;");

        // the replacement and the old contents overlap, which also proves the
        // observer saw both mutations rather than the counts being untouched
        Assert.assertEquals(
                "The replacement should be added before the old contents are removed",
                2L, maxChildCount.longValue());
        Assert.assertEquals(
                "The container should never be emptied while its contents are replaced",
                1L, minChildCount.longValue());
    }
}
