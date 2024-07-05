/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;

public class ExtraNodeTestComponents {

    @JsModule("@polymer/a.js")
    public class ExtraJsModuleComponent extends Component {
    }

    @JavaScript("./extra-javascript.js")
    public class ExtraJavaScriptComponent extends Component {
    }

    @CssImport(value = "./extra-css.css", themeFor = "extra-foo", include = "extra-bar")
    public static class ExtraCssImport extends Component {
    }

}
