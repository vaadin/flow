package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import java.util.Enumeration;

public class VaadinServletConfig implements VaadinConfig {

    private ServletConfig config;

    public VaadinServletConfig(ServletConfig config) {
        this.config = config;
    }

    /**
     * Ensures there is a valid instance of {@link ServletConfig}.
     */
    private void ensureServletConfig() {
        if(config == null && VaadinService.getCurrent() instanceof VaadinServletService) {
            config = ((VaadinServletService)VaadinService.getCurrent()).getServlet().getServletConfig();
        } else if(config == null) {
            throw new IllegalStateException("The underlying ServletContext of VaadinServletContext is null and there is no VaadinServletService to obtain it from.");
        }
    }

    @Override
    public VaadinContext getVaadinContext() {
        return new VaadinServletContext(config.getServletContext());
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        ensureServletConfig();
        return config.getInitParameterNames();
    }

    @Override
    public String getInitParameter(String name) {
        ensureServletConfig();
        return config.getInitParameter(name);
    }
}
