package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class IFrameIT extends ChromeBrowserTest {
    private IFrameTestView testView = new IFrameTestView();

    @Test
    public void testIFrameReload() {
        open();

        assertTrue(testView.frame.getSrcdoc().get().contains("This is page A"));

        testView.handleButtonClick();

        assertTrue(testView.frame.getSrcdoc().get().contains("This is not page A"));

    }
}
