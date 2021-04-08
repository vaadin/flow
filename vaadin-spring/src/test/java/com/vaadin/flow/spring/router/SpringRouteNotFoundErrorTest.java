package com.vaadin.flow.spring.router;

import java.util.ArrayList;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.SpringBootVersion;

import static org.mockito.Mockito.when;

public class SpringRouteNotFoundErrorTest {

    @Mock
    private BeforeEnterEvent event;

    @Mock
    private ErrorParameter<NotFoundException> parameter;

    @Mock
    private UI ui;

    @Mock
    private VaadinSession session;

    @Mock
    private DeploymentConfiguration configuration;

    @Mock
    private Router router;

    @Mock
    private RouteRegistry registry;
    private AutoCloseable autoCloseable;

    @Before
    public void setup() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        when(event.getLocation()).thenReturn(new Location("foobar"));
        when(parameter.hasCustomMessage()).thenReturn(false);
        when(event.getUI()).thenReturn(ui);
        when(ui.getSession()).thenReturn(session);
        when(session.getConfiguration()).thenReturn(configuration);
        when(configuration.isProductionMode()).thenReturn(false);
        when(event.getSource()).thenReturn(router);
        when(router.getRegistry()).thenReturn(registry);
        when(registry.getRegisteredRoutes()).thenReturn(new ArrayList<>());
    }

    @After
    public void teardown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void testRouteNotFoundError_bootVersionIsTwoFourOrNewer_noAddedMessageShown() {
        final RouteNotFoundError normalErrorView = new RouteNotFoundError();
        final SpringRouteNotFoundError springRouteNotFoundError = new SpringRouteNotFoundError();

        normalErrorView.setErrorParameter(event, parameter);
        // the project uses 2.4.0 at the time of writing this test, so it will
        // not add the message
        springRouteNotFoundError.setErrorParameter(event, parameter);

        Assert.assertEquals("Invalid number of children",
                ((Long) normalErrorView.getChildren().count()).intValue(),
                ((Long) springRouteNotFoundError.getChildren().count())
                        .intValue());
    }

    @Test
    public void testRouteNotFoundError_bootVersionIsTwoTwoSomething_addedMessageShown() {
        final RouteNotFoundError normalErrorView = new RouteNotFoundError();
        final SpringRouteNotFoundError springRouteNotFoundError = new SpringRouteNotFoundError();

        try (MockedStatic<SpringBootVersion> theMock = Mockito
                .mockStatic(SpringBootVersion.class)) {
            theMock.when(SpringBootVersion::getVersion)
                    .thenReturn("2.2.0.RELEASE");

            normalErrorView.setErrorParameter(event, parameter);
            springRouteNotFoundError.setErrorParameter(event, parameter);

            theMock.verify(SpringBootVersion::getVersion);
        }
        Assert.assertNotEquals("Invalid number of children",
                ((Long) normalErrorView.getChildren().count()).intValue(),
                ((Long) springRouteNotFoundError.getChildren().count())
                        .intValue());
    }

    @Test
    public void testRouteNotFoundError_productionMode_SpringVersionNotChecked() {
        final SpringRouteNotFoundError springRouteNotFoundError = new SpringRouteNotFoundError();

        try (MockedStatic<SpringBootVersion> theMock = Mockito
                .mockStatic(SpringBootVersion.class)) {
            when(configuration.isProductionMode()).thenReturn(true);
            theMock.when(SpringBootVersion::getVersion)
                    .then(AssertionError::new);
            springRouteNotFoundError.setErrorParameter(event, parameter);
            theMock.verifyNoInteractions();
        }
    }
}
