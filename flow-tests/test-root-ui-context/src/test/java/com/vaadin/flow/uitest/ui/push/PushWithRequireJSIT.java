package com.vaadin.flow.uitest.ui.push;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(PushTests.class)
public class PushWithRequireJSIT extends ChromeBrowserTest {

    @Test
    public void pushWithRequireJS() {
        open();

        checkLogsForErrors(msg -> msg.contains("sockks-node"));
    }

}