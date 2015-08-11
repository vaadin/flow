package com.vaadin.tests.components.datefield;

import java.util.Date;
import java.util.Locale;

import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.tests.components.AbstractTestCase;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.DateField;
import com.vaadin.ui.GridLayout;

public class DateFieldPopupOffScreen extends AbstractTestCase {

    @Override
    protected String getTestDescription() {
        return "Test for the popup position from a DateField. The popup should always be on-screen even if the DateField is close the the edge of the browser.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 3639;
    }

    @Override
    public void init(VaadinRequest r) {
        GridLayout mainLayout = new GridLayout(3, 3);
        mainLayout.setSizeFull();

        DateField df;

        df = createDateField();
        mainLayout.addComponent(df, 2, 0);
        mainLayout.setComponentAlignment(df, Alignment.TOP_RIGHT);

        df = createDateField();
        mainLayout.addComponent(df, 2, 1);
        mainLayout.setComponentAlignment(df, Alignment.MIDDLE_RIGHT);

        df = createDateField();
        mainLayout.addComponent(df, 2, 2);
        mainLayout.setComponentAlignment(df, Alignment.BOTTOM_RIGHT);

        df = createDateField();
        mainLayout.addComponent(df, 0, 2);
        mainLayout.setComponentAlignment(df, Alignment.BOTTOM_LEFT);

        df = createDateField();
        mainLayout.addComponent(df, 1, 2);
        mainLayout.setComponentAlignment(df, Alignment.BOTTOM_CENTER);

        setContent(mainLayout);
    }

    private DateField createDateField() {
        DateField df = new DateField();
        df.setLocale(new Locale("fi"));
        df.setResolution(Resolution.SECOND);
        df.setValue(new Date(1000000L));
        return df;
    }
}
