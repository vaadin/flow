package com.vaadin.flow.contexttest.ui;

public class SubContextNoEndingSlashIT extends SubContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context";
    }

}
