/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.Locale;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Direction;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DirectionChangeView", layout = ViewTestLayout.class)
public class DirectionChangeView extends AbstractDivView
        implements HasUrlParameter<String>, LocaleChangeObserver {

    private Div locale = new Div();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        locale.setText(attachEvent.getUI().getLocale().toString());
        locale.setId("initial-direction");

        NativeButton changeLocale = new NativeButton(
                "Swap location to " + Locale.SIMPLIFIED_CHINESE,
                event -> attachEvent.getUI()
                        .setLocale(Locale.SIMPLIFIED_CHINESE));
        changeLocale.setId("locale-button");

        NativeButton ltrButton = new NativeButton(
                "Swap to " + Direction.LEFT_TO_RIGHT, event -> attachEvent
                        .getUI().setDirection(Direction.LEFT_TO_RIGHT));
        ltrButton.setId("ltr-button");
        NativeButton rtlButton = new NativeButton(
                "Swap to " + Direction.RIGHT_TO_LEFT, event -> attachEvent
                        .getUI().setDirection(Direction.RIGHT_TO_LEFT));
        rtlButton.setId("rtl-button");
        add(locale, ltrButton, rtlButton, changeLocale);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        String queryString = event.getLocation().getQueryParameters()
                .getQueryString();
        if ("rtl".equals(queryString)) {
            event.getUI().setDirection(Direction.RIGHT_TO_LEFT);
        } else if ("ltr".equals(queryString)) {
            event.getUI().setDirection(Direction.LEFT_TO_RIGHT);
        }
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        if (event.getLocale() == Locale.SIMPLIFIED_CHINESE) {
            event.getUI().setDirection(Direction.RIGHT_TO_LEFT);
        }
        locale.setText(event.getLocale().toString());
    }
}
