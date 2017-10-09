package com.vaadin.flow.tutorial.routing;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.router.HasDynamicTitle;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.Route;
import com.vaadin.router.PageTitle;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.html.Div;

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
        public void setParameter(BeforeNavigationEvent event,
                @com.vaadin.router.OptionalParameter Long parameter) {
            if (parameter != null) {
                title = "Blog Post #" + parameter;
            } else {
                title = "Blog Home";
            }
        }
    }
}
