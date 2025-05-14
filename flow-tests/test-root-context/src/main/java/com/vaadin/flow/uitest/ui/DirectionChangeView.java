/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
