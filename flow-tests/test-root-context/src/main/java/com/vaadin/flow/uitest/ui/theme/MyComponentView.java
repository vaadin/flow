package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.theme.MyComponentView")
public class MyComponentView extends Div {

    public MyComponentView() {
        add(new MyComponent());
    }
}
