/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;

public class MultipleAnchorsIT extends AbstractScrollIT {

    @Test
    public void numerousDifferentAnchorsShouldWorkAndHistoryShouldBePreserved() {
        testBench().resizeViewPortTo(700, 600);
        open();

        final Long initialHistoryLength = getBrowserHistoryLength();

        int anchorIndex = 0;
        while (anchorIndex < MultipleAnchorsView.NUMBER_OF_ANCHORS) {
            clickElementWithJs(
                    MultipleAnchorsView.ANCHOR_URL_ID_BASE + anchorIndex);
            verifyAnchor(anchorIndex);
            anchorIndex++;
        }

        assertThat(
                "Browser history length should be increased by number of anchor urls="
                        + MultipleAnchorsView.NUMBER_OF_ANCHORS,
                getBrowserHistoryLength(), is(initialHistoryLength
                        + MultipleAnchorsView.NUMBER_OF_ANCHORS));

        anchorIndex--;

        while (anchorIndex > 0) {
            anchorIndex--;
            driver.navigate().back();
            verifyAnchor(anchorIndex);
        }
    }

    @Test
    public void numerousEqualAnchorsShouldRepresentOneHistoryEntry() {
        testBench().resizeViewPortTo(700, 800);
        open();

        final Long initialHistoryLength = getBrowserHistoryLength();
        final String initialUrl = driver.getCurrentUrl();
        final int indexToClick = 2;

        for (int i = 0; i < 10; i++) {
            clickElementWithJs(
                    MultipleAnchorsView.ANCHOR_URL_ID_BASE + indexToClick);
            verifyAnchor(indexToClick);
        }

        assertThat(
                "Browser history length should be increased by 1 (number of different anchor urls used)",
                getBrowserHistoryLength(), is(initialHistoryLength + 1));

        driver.navigate().back();
        assertThat("Expected to have initialUrl", driver.getCurrentUrl(),
                is(initialUrl));
        ensureThatNewPageIsNotScrolled();
    }

    private void verifyAnchor(int idNumber) {
        Point anchorElementLocation = findElement(
                By.id(MultipleAnchorsView.ANCHOR_DIV_ID_BASE + idNumber))
                .getLocation();
        assertThat("Expected url to change to anchor one",
                driver.getCurrentUrl(),
                endsWith(MultipleAnchorsView.ANCHOR_URL_BASE + idNumber));
        checkPageScroll(anchorElementLocation.getX(),
                anchorElementLocation.getY());
    }

    private Long getBrowserHistoryLength() {
        return (Long) executeScript("return window.history.length");
    }
}
