/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;

@Route(value = "com.vaadin.flow.uitest.ui.theme.NpmThemedComponentView")
@Theme(MyTheme.class)
@Tag("npm-themed-component")
@JsModule("./src/npm-themed-component.js")
public class NpmThemedComponentView extends Component {

}
