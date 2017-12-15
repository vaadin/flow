package com.vaadin.flow.tutorial.misc;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.LoadingIndicatorConfiguration;
import com.vaadin.ui.UI;

@CodeFor("miscellaneous/tutorial-loading-indicator.asciidoc")
public class LoadingIndicator {
    public class MyUI extends UI {
        @Override
        protected void init(VaadinRequest request) {
            LoadingIndicatorConfiguration conf = getLoadingIndicatorConfiguration();
            //@formatter:off
            /* Delay for showing the indicator and setting the 'first' class name. */
            conf.setFirstDelay(300); // 300ms is the default

            /* Delay for setting the 'second' class name */
            conf.setSecondDelay(1500); // 1500ms is the default

            /* Delay for setting the 'third' class name */
            conf.setThirdDelay(5000); // 5000ms is the default
            //@formatter:on
        }
    }
}
