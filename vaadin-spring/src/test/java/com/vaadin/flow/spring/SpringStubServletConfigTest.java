package com.vaadin.flow.spring;

import javax.servlet.ServletContext;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;

import static org.mockito.Mockito.when;

/**
 * Test for checking that styb servlet configuration collects expected values.
 */
public class SpringStubServletConfigTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Environment environment;

    @Mock
    private ServletContext context;

    @Mock
    private ServletRegistrationBean registration;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(applicationContext.getBean(Environment.class))
                .thenReturn(environment);
        when(registration.getServletName()).thenReturn("vaadinServlet");
        when(context.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        when(registration.getInitParameters())
                .thenReturn(Collections.emptyMap());

    }

    @Test
    public void vaadinProductionMode_isReadFromEnvironmentVariables() {
        DeploymentConfiguration deploymentConfiguration = VaadinServletContextInitializer.SpringStubServletConfig
                .createDeploymentConfiguration(context, registration,
                        SpringServlet.class, applicationContext);

        Assert.assertFalse("ProductionMode should be 'false' by default.",
                deploymentConfiguration.isProductionMode());

        when(environment.getProperty(
                "vaadin." + Constants.SERVLET_PARAMETER_PRODUCTION_MODE))
                        .thenReturn("true");

        deploymentConfiguration = VaadinServletContextInitializer.SpringStubServletConfig
                .createDeploymentConfiguration(context, registration,
                        SpringServlet.class, applicationContext);

        Assert.assertTrue(
                "ProductionMode should have been 'true' as it was in the environment.",
                deploymentConfiguration.isProductionMode());
    }
}
