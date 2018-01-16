package com.vaadin.flow.tutorial.advanced;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("advanced/tutorial-i18n-localization.asciidoc")
@WebServlet(urlPatterns = "/*", name = "slot", asyncSupported = true, initParams = {
        @WebInitParam(name = Constants.I18N_PROVIDER, value = "com.vaadin.example.ui.TranslationProvider") })
public class ApplicationServlet extends VaadinServlet {
}
