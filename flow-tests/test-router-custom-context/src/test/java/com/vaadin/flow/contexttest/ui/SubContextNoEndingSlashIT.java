package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class SubContextNoEndingSlashIT extends SubContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context";
    }


}
