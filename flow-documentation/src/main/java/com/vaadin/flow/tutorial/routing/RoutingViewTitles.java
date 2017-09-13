package com.vaadin.flow.tutorial.routing;

import com.vaadin.annotations.Title;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.DefaultPageTitleGenerator;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.View;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-view-titles.asciidoc")
public class RoutingViewTitles {
    @Title("home")
    class HomeView extends Div implements View {

        HomeView() {
            setText("This is the home view");
        }

    }

    class ProductView extends Div implements View {

        ProductView() {
            setText("This is the Products view");
        }

        @Override
        public String getTitle(LocationChangeEvent event) {
            // Default implementation returns "" which clears any previous
            // title.
            return "Product " + getProductName(event.getPathParameter("id"));
        }

    }

    public String getProductName(String idParameter) {
        // TODO Auto-generated method stub
        return null;
    }

    class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            // setRoute calls omitted

            // setting a custom generator
            configuration.setPageTitleGenerator(new CustomPageTitleGenerator());
        }

    }

    class CustomPageTitleGenerator extends DefaultPageTitleGenerator {

        @Override
        public String getPageTitle(LocationChangeEvent event) {
            // use the annotation / getTitle() value if applicable
            String annotationValue = super.getPageTitle(event);
            if (annotationValue.isEmpty()) {
                // use default title for views not using @Title or getTitle()
                annotationValue = "Default Page Title";
            }
            return annotationValue;
        }
    }
}
