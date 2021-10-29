package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.DevModeGizmoElement;

import org.junit.Assert;
import org.junit.Test;

public class DebugWindowFeatureFlagsIT extends ChromeBrowserTest {
    @Override
    protected Class<? extends Component> getViewClass() {
        return DebugWindowErrorHandlingView.class;
    }

    @Test
    public void exampleFeatureFlagNotShown() {
        open();
        DevModeGizmoElement debugWindow = $(DevModeGizmoElement.class).first();
        debugWindow.expand();
        debugWindow.showExperimentalFeatures();
        List<String> features = debugWindow.listExperimentalFeatures();
        for (String feature : features) {
            Assert.assertFalse(
                    "Example feature should not be shown in the debug window",
                    feature.contains("Example feature"));
        }
    }
}
