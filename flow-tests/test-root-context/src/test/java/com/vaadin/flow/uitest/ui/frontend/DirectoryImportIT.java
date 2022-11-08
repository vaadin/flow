package com.vaadin.flow.uitest.ui.frontend;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class DirectoryImportIT extends ChromeBrowserTest {

    @Test
    public void directoryImportWorks() {
        open();

        TestBenchElement component = $("a-directory-component").first();
        Assert.assertEquals("Directory import ok", component.getText());
    }

}
