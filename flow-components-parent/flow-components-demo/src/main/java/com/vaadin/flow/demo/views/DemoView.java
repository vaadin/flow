package com.vaadin.flow.demo.views;

import com.vaadin.flow.router.View;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;

public abstract class DemoView extends Component
        implements View, HasComponents {

    public abstract String getViewName();

}
