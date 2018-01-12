package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Theme;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.theme.MyThemeComponentView")
@Theme(MyTheme.class)
public class MyThemeComponentView extends Div {

    public MyThemeComponentView() {
        add(new MyComponent());
    }
}
