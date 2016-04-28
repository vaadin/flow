package com.vaadin.humminbird.tutorial.routing;

import com.vaadin.annotations.Title;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.router.DefaultPageTitleGenerator;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.hummingbird.router.View;

@CodeFor("tutorial-routing-view-titles.asciidoc")
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
