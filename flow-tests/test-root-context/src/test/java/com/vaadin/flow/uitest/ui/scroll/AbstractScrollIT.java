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
package com.vaadin.flow.uitest.ui.scroll;

import static org.junit.Assert.assertThat;

import org.hamcrest.number.IsCloseTo;

import com.vaadin.flow.testutil.ChromeBrowserTest;

abstract class AbstractScrollIT extends ChromeBrowserTest {

    protected static final int SCROLL_DELTA = 2;

    protected void checkPageScroll(int x, int y, int delta) {
        assertThat("Unexpected x scroll position", (double) getScrollX(),
                IsCloseTo.closeTo(x, delta));
        assertThat("Unexpected y scroll position", (double) getScrollY(),
                IsCloseTo.closeTo(y, delta));
    }

    protected void checkPageScroll(int x, int y) {
        checkPageScroll(x, y, SCROLL_DELTA);
    }

    protected void ensureThatNewPageIsNotScrolled() {
        checkPageScroll(0, 0, 0);
    }
}
