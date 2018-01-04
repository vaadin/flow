package com.vaadin.flow.tutorial.routing;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-page-titles.asciidoc")
public class RoutingViewTitles {
    @PageTitle("home")
    class HomeView extends Div {

        HomeView() {
            setText("This is the home view");
        }

    }

    @Route(value = "blog")
    class BlogPost extends Component
            implements HasDynamicTitle, HasUrlParameter<Long> {
        private String title = "";

        @Override
        public String getPageTitle() {
            return title;
        }

        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter Long parameter) {
            if (parameter != null) {
                title = "Blog Post #" + parameter;
            } else {
                title = "Blog Home";
            }
        }
    }
}
