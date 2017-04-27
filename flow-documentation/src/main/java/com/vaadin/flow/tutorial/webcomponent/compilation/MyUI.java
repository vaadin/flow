package com.vaadin.flow.tutorial.webcomponent.compilation;

import com.vaadin.annotations.WebComponents;
import com.vaadin.annotations.WebComponents.PolyfillVersion;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.UI;

@CodeFor("tutorial-webcomponents-es5.asciidoc")
@WebComponents(PolyfillVersion.V1)
public class MyUI extends UI {
}
