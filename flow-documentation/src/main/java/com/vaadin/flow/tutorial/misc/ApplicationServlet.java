package com.vaadin.flow.tutorial.misc;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServlet;

@CodeFor("miscellaneous/tutorial-i18n-localization.asciidoc")
@WebServlet(urlPatterns = "/*", name = "slot", asyncSupported = true, initParams = {
        @WebInitParam(name = Constants.I18N_PROVIDER, value = "com.vaadin.example.ui.TranslationProvider") })
public class ApplicationServlet extends VaadinServlet {
}
