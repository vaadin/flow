package com.vaadin.humminbird.tutorial.misc;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.LoadingIndicatorConfiguration;
import com.vaadin.ui.UI;

@CodeFor("tutorial-loading-indicator.asciidoc")
public class LoadingIndicator {
    public class MyUI extends UI {
        @Override
        protected void init(VaadinRequest request) {
            LoadingIndicatorConfiguration conf = getLoadingIndicatorConfiguration();
            //@formatter:off
            /* The delay after which the indicator is shown */
            conf.setFirstDelay(300); // 300ms is the default

            /* The delay after which the indicator gets the 'second' class name */
            conf.setSecondDelay(1500); // 1500ms is the default

            /* The delay after which the indicator gets the 'third' class name */
            conf.setThirdDelay(5000); // 5000ms is the default
    }}
    //@formatter:on
}
