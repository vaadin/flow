package com.vaadin.test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;

@Route("project-route")
@Tag("div")
public class ProjectRoot extends Component implements HasComponents {

    public ProjectRoot() {
        add(new ProjectComponent());
    }
}
