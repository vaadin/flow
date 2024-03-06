/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
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

    @JsModule("@polymer/b.js")
    public class ExtraJsModuleComponent2 extends Component {
    }

    @JsModule("@polymer/c.js")
    public class ExtraJsModuleComponent3 extends Component {
    }

    @JsModule("@polymer/D.js")
    public class ExtraJsModuleComponent4 extends Component {
    }

    @JsModule("@polymer/e.js")
    public class ExtraJsModuleComponent5 extends Component {
    }

    @JavaScript("./extra-javascript.js")
    public class ExtraJavaScriptComponent extends Component {
    }

    @CssImport(value = "./extra-css.css", themeFor = "extra-foo", include = "extra-bar")
    public static class ExtraCssImport extends Component {
    }

    @CssImport(value = "./a-css.css", include = "a-a")
    public static class ExtraCssImport2 extends Component {
    }

    @CssImport(value = "./b-css.css")
    public static class ExtraCssImport3 extends Component {
    }

}
