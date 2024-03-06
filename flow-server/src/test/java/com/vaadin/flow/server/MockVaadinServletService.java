/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletException;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.tests.util.MockDeploymentConfiguration;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockVaadinServletService extends VaadinServletService {

    private Instantiator instantiator;

    private Router router;

    private ResourceProvider resourceProvider;

    private static class MockVaadinServlet extends VaadinServlet {

        private final DeploymentConfiguration configuration;

        private VaadinServletService service;

        private MockVaadinServlet(DeploymentConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        protected DeploymentConfiguration createDeploymentConfiguration()
                throws ServletException {
            return configuration;
        }

        @Override
        protected VaadinServletService createServletService(
                DeploymentConfiguration deploymentConfiguration)
                throws ServiceException {
            return service;
        }

    }

    public MockVaadinServletService() {
        this(new MockDeploymentConfiguration());
    }

    public MockVaadinServletService(
            DeploymentConfiguration deploymentConfiguration) {
        super(new MockVaadinServlet(deploymentConfiguration),
                deploymentConfiguration);
        init();
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    @Override
    public Router getRouter() {
        return router != null ? router : super.getRouter();
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        return Collections.emptyList();
    }

    public void init(Instantiator instantiator) {
        this.instantiator = instantiator;

        init();
    }

    @Override
    protected Instantiator createInstantiator() throws ServiceException {
        if (instantiator != null) {
            return instantiator;
        }
        return super.createInstantiator();
    }

    @Override
    public void init() {
        try {
            MockVaadinServlet servlet = (MockVaadinServlet) getServlet();
            servlet.service = this;
            if (getServlet().getServletConfig() == null) {
                getServlet().init(new MockServletConfig());
            }
            super.init();
        } catch (ServiceException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

}
