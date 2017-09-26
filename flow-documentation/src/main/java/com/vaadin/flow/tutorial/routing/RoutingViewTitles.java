package com.vaadin.flow.tutorial.routing;

import com.vaadin.router.Title;
import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.DefaultPageTitleGenerator;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.View;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-page-titles.asciidoc")
public class RoutingViewTitles {
    @Title("home")
    class HomeView extends Div implements View {

        HomeView() {
            setText("This is the home view");
        }

    }
}
