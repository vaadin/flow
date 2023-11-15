package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.testutil.ChromeBrowserTest;

abstract class AbstractBaseIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.replace(view, "path/");
    }

}
