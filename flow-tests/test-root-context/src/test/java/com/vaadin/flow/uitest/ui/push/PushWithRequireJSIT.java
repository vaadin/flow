package com.vaadin.flow.uitest.ui.push;

import org.junit.jupiter.api.Tag;

import com.vaadin.flow.testutil.TestTag;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;

@Tag(TestTag.PUSH_TESTS)
public class PushWithRequireJSIT extends ChromeBrowserTest {

    @BrowserTest
    public void pushWithRequireJS() {
        open();

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
    }

}
