/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.flow.testonlynpmfeatures.defaulttheme;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.testonlynpmfeatures.defaulttheme.DefaultThemeView")
@JsModule("styles/styles.js")
public class DefaultThemeView extends Div {
    public DefaultThemeView() {
        Anchor anchor = new Anchor("https://www.google.com", "Google");
        add(anchor);
        add(new TemplateWithClientSideImports());
    }
}
