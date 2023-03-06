package com.vaadin.flow.frontend;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;

@CssImport("./addons-styles/add-on-styles.css")
public class MyComponent extends Div {

    public MyComponent() {
        setText("Test CssImport from META-INF/resources with dev bundle");
        addClassName("my-component");
    }
}
