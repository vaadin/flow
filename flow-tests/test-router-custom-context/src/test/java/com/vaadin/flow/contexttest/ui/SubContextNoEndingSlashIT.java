package com.vaadin.flow.contexttest.ui;

import org.junit.Ignore;

@Ignore
public class SubContextNoEndingSlashIT extends SubContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context";
    }


}
