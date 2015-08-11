package com.vaadin.tests.components.nativeselect;

import java.util.LinkedHashMap;

import com.vaadin.tests.components.select.AbstractSelectTestCase;
import com.vaadin.ui.NativeSelect;

public class NativeSelects extends AbstractSelectTestCase<NativeSelect> {

    @Override
    protected Class<NativeSelect> getTestClass() {
        return NativeSelect.class;
    }

    @Override
    protected void createActions() {
        super.createActions();
        createColumnSelectAction();
    }

    private void createColumnSelectAction() {
        LinkedHashMap<String, Integer> options = new LinkedHashMap<String, Integer>();
        options.put("-", 0);
        for (int i = 1; i <= 10; i++) {
            options.put(String.valueOf(i), i);
        }
        options.put("50", 50);
        options.put("100", 100);
        options.put("1000", 1000);

    }

}
