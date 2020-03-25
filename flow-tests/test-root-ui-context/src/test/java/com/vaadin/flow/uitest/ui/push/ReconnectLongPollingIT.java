package com.vaadin.flow.uitest.ui.push;

import org.junit.Ignore;

import com.vaadin.flow.component.UI;

@Ignore("see https://github.com/vaadin/flow/issues/7878."
        + " The test is failing in 3.0 branch because there a bug in this branch.")
public class ReconnectLongPollingIT extends ReconnectTest {

    @Override
    protected Class<? extends UI> getUIClass() {
        return BasicPushLongPollingUI.class;
    }

}
