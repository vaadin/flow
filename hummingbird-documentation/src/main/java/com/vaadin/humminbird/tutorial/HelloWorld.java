package com.vaadin.humminbird.tutorial;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

public class HelloWorld {

    public class HelloWorldUI extends UI {

        @WebServlet(urlPatterns = "/*", name = "HelloWorldServlet")
        @VaadinServletConfiguration(ui = HelloWorldUI.class, productionMode = false)
        public class HelloWorldServlet extends VaadinServlet {
        }

        @Override
        protected void init(VaadinRequest request) {
            Element div = new Element("div");
            div.setTextContent("Hello world");
            getElement().appendChild(div);
        }
    }

}
