package com.vaadin.tests.components.uitest.components;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.UserError;
import com.vaadin.tests.components.uitest.TestSampler;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.ValoTheme;

public class TabSheetsCssTest {

    private TestSampler parent;
    private int debugIdCounter = 0;

    public TabSheetsCssTest(TestSampler parent) {
        this.parent = parent;

        TabSheet basic = createTabSheetWith("Basic TabSheet", null);
        parent.addComponent(basic);

        TabSheet bar = createTabSheetWith("A small/bar TabSheet",
                ValoTheme.TABSHEET_COMPACT_TABBAR);
        parent.addComponent(bar);

        TabSheet selectedClosable = createTabSheetWith(
                "A selected-closable TabSheet",
                ValoTheme.TABSHEET_ONLY_SELECTED_TAB_IS_CLOSABLE);
        parent.addComponent(selectedClosable);

    }

    private TabSheet createTabSheetWith(String caption, String styleName) {
        TabSheet ts = new TabSheet();
        ts.setId("tabsheet" + debugIdCounter++);
        ts.setCaption(caption);
        ts.setComponentError(new UserError("A error message"));

        Label content = new Label("First Component");
        ts.addTab(content, "First");
        Label content2 = new Label("Second Component");
        ts.addTab(content2, "Second");
        ts.getTab(content2).setClosable(true);

        Label content3 = new Label("Third Component");
        ts.addTab(content3, "Third", new ExternalResource(parent.ICON_URL));
        ts.getTab(content3).setEnabled(false);

        if (styleName != null) {
            ts.addStyleName(styleName);
        }

        return ts;

    }
}
