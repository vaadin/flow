package com.vaadin.flow.contexttest.ui;

public class SubContextSubPathIT extends SubContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context/foo/bar";
    }

}
