package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class SubContextSubPathIT extends SubContextIT {

    @Override
    protected String getAppContext() {
        return "/sub-context/foo/bar";
    }

}
