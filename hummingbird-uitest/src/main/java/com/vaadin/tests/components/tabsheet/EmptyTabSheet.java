package com.vaadin.tests.components.tabsheet;

import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.TabSheet;

public class EmptyTabSheet extends TestBase {

    private TabSheet tabSheet;

    @Override
    protected String getTestDescription() {
        return "Test a TabSheet without any tabs.";
    }

    @Override
    protected Integer getTicketNumber() {
        return null;
    }

    @Override
    protected void setup() {
        tabSheet = new TabSheet();
        tabSheet.setId("tabsheet");
        add(tabSheet);
    }

}
