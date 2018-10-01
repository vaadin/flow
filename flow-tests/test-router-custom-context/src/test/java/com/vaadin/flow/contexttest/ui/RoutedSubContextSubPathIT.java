package com.vaadin.flow.contexttest.ui;

public class RoutedSubContextSubPathIT extends RoutedSubContextIT {

    @Override
    protected String getAppContext() {
        return "/routed/path-sub-context/foo/bar";
    }

}
