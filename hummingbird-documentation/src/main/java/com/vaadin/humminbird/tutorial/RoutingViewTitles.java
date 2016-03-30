package com.vaadin.humminbird.tutorial;

import com.vaadin.annotations.Title;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.router.DefaultPageTitleGenerator;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.router.ModifiableRouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.hummingbird.router.View;

@CodeFor("tutorial-routing-view-titles.asciidoc")
public class RoutingViewTitles {
    @Title("home")
    class HomeView implements View {

        @Override
        public Element getElement() {
            return ElementFactory.createDiv("This is the home view");
        }
    }

    class ProductView implements View {

        @Override
        public String getTitle(LocationChangeEvent event) {
            // Default implementation returns "" which clears any previous title.
            return "Product " + getProductName();
        }

        @Override
        public Element getElement() {
            return ElementFactory.createDiv("This is the Products view");
        }
    }

    public String getProductName() {
        // TODO Auto-generated method stub
        return null;
    }

    class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(ModifiableRouterConfiguration configuration) {
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
