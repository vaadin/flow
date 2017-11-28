package com.vaadin.guice.server;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServletService;

import java.util.Optional;

class GuiceVaadinServletService extends VaadinServletService {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Instantiator> instantiator;

    /**
     * Creates an instance connected to the given servlet and using the given
     * configuration with provided application {@code context}.
     *
     * @param servlet
     *            the servlet which receives requests
     * @param deploymentConfiguration
     *            the configuration to use
     */
    public GuiceVaadinServletService(GuiceVaadinServlet servlet, DeploymentConfiguration deploymentConfiguration) throws ServiceException {
        super(servlet, deploymentConfiguration);
        this.instantiator = Optional.of(new GuiceInstantiator(this));
        init();
    }

    @Override
    protected Optional<Instantiator> loadInstantiators() {
        return instantiator;
    }
}
