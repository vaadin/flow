package com.vaadin.tests.components.combobox;

import com.vaadin.server.ThemeResource;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;

@SuppressWarnings("serial")
public class RemovalOfSelectedIcon extends TestBase {

    @Override
    protected void setup() {

        final ComboBox cb1 = createComboBox("Don't touch this combobox");
        add(cb1);

        final ComboBox cb2 = createComboBox("Select icon test combobox");
        add(cb2);

        Button btClear = new Button("Clear button");
        btClear.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                cb2.removeAllItems();
                cb2.setContainerDataSource(null);
            }
        });

        add(btClear);
    }

    private ComboBox createComboBox(String caption) {
        ComboBox cb = new ComboBox(caption);
        cb.addItem(1);
        cb.setItemCaption(1, "icon test");
        cb.setItemIcon(1, new ThemeResource("menubar/img/checked.png"));
        return cb;
    }

    @Override
    protected String getTestDescription() {
        return "Clear button must remove selected icon, and comboboxes' widths must stay same.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 4353;
    }

}
