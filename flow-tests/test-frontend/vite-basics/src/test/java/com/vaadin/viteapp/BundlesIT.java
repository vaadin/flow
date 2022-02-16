package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.DevModeGizmoElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.viteapp.views.empty.MainView;

public class BundleIT extends ViteDevModeIT {

    @Test
    public void bundleButtonIsFromBundle() {
        Assert.assertTrue(
                (Boolean) $("bundle-button").first().getProperty(
                        "isFromBundle")
        );
    }
}
