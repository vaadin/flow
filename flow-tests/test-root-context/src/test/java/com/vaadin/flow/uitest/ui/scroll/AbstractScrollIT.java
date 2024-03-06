/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
