package com.vaadin.hummingbird.demo.expensemanager;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

/**
 * Created by Mikael on 24/02/17.
 */
public class ExpenseManagerUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        super.init(request);

        MemoryUsageMonitor.registerUI(this);
    }
}
