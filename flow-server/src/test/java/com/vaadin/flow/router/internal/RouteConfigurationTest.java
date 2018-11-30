package com.vaadin.flow.router.internal;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.startup.RouteTarget;

public class RouteConfigurationTest {

    private RouteConfiguration configuration;

    @Before
    public void init() {
        configuration = new RouteConfiguration();
        Assert.assertFalse("Initial configuration was not immutable!",
                configuration.isMutable());
    }

    @Test
    public void mutableConfiguration_canSetRouteTarget() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);
        Assert.assertTrue("Configuration should be mutable.",
                mutable.isMutable());

        RouteTarget target = new RouteTarget(BaseTarget.class);
        mutable.setRouteTarget("", target);

        Assert.assertTrue("Configuration should have \"\" route registered",
                mutable.hasRoute(""));
        Assert.assertTrue("Exact match with no path segments should exist",
                mutable.hasRoute("", Collections.EMPTY_LIST));
        Assert.assertEquals("Configuration should have registered base target.",
                BaseTarget.class,
                mutable.getRoute("", Collections.EMPTY_LIST).get());
    }

    @Test
    public void mutableConfiguration_canSetTargetRoute() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);
        Assert.assertTrue("Configuration should be mutable.",
                mutable.isMutable());

        mutable.setTargetRoute(BaseTarget.class, "");

        Assert.assertTrue("", mutable.hasRouteTarget(BaseTarget.class));

        Assert.assertEquals("Configuration should have registered base target.",
                "", mutable.getTargetRoute(BaseTarget.class));
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        RouteTarget target = new RouteTarget(BaseTarget.class);
        mutable.setRouteTarget("", target);
        mutable.setTargetRoute(BaseTarget.class, "");

        RouteConfiguration immutable = new RouteConfiguration(mutable, false);
        Assert.assertFalse(
                "Configuration is mutable even though it should be immutable,",
                immutable.isMutable());

        Assert.assertTrue("Configuration should have \"\" route registered",
                immutable.hasRoute(""));
        Assert.assertTrue("Exact match with no path segments should exist",
                immutable.hasRoute("", Collections.EMPTY_LIST));
        Assert.assertEquals("Configuration should have registered base target.",
                BaseTarget.class,
                immutable.getRoute("", Collections.EMPTY_LIST).get());

        Assert.assertTrue("", immutable.hasRouteTarget(BaseTarget.class));
        Assert.assertEquals("Configuration should have registered base target.",
                "", immutable.getTargetRoute(BaseTarget.class));
    }

    @Test
    public void emptyConfiguration_allGetMethodsWork() {
        //        RouteConfiguration configuration = new RouteConfiguration();

        Assert.assertFalse("No routes should be configured",
                configuration.hasRoute(""));
        Assert.assertFalse("No routes should be configured",
                configuration.hasRoute("", Collections.emptyList()));
        Assert.assertFalse("No routes should be configured",
                configuration.getRoute("", Collections.emptyList())
                        .isPresent());
        Assert.assertTrue("Configuration should be empty",
                configuration.isEmpty());
        Assert.assertTrue("Configuration should be empty",
                configuration.getTargetRoutes().isEmpty());
        Assert.assertFalse("Configuration should be empty",
                configuration.hasExceptionTargets());
        Assert.assertNull("No exception handler should be found.", configuration
                .getExceptionHandlerByClass(RuntimeException.class));
        Assert.assertNull("No target route should be found",
                configuration.getTargetRoute(BaseTarget.class));
        Assert.assertTrue("Configuration should be empty",
                configuration.getExceptionHandlers().isEmpty());
        Assert.assertFalse("No route should be found",
                configuration.hasRouteTarget(BaseTarget.class));
    }

    @Test
    public void immutableConfiguration_allSetMethodsThrow() {
        //        RouteConfiguration configuration = new RouteConfiguration();

        try {
            configuration.clear();
            Assert.fail(
                    "Configuration could be cleared even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }
        try {
            configuration.addRouteTarget("home", BaseTarget.class);
            Assert.fail(
                    "Configuration could add a new target route even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration
                    .setRouteTarget("home", new RouteTarget(BaseTarget.class));
            Assert.fail(
                    "Configuration could add a RouteTarget even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.setTargetRoute(BaseTarget.class, "home");
            Assert.fail(
                    "Configuration could add a RouteTarget even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.setErrorRoute(UnsupportedOperationException.class,
                    BaseTarget.class);
            Assert.fail(
                    "Configuration could add a exception handler even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.removeRoute(BaseTarget.class);
            Assert.fail(
                    "Configuration could remove route by class even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.removeRoute("home");
            Assert.fail(
                    "Configuration could remove route by string even if it should throw in immutable state!");
        } catch (IllegalStateException ise) {

        }
    }

    @Tag("div")
    public static class BaseTarget extends Component {
    }

    @Tag("div")
    public static class ParamTarget extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }
}