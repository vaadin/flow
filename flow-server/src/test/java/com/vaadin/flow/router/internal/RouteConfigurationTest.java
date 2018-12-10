package com.vaadin.flow.router.internal;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
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

        mutable.setRoute("", BaseTarget.class);

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

        mutable.setRoute("", BaseTarget.class);
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
    public void mutableConfiguration_canSetErrorTargets() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assert.assertTrue("Exception targets should be available",
                mutable.hasExceptionTargets());
        Assert.assertEquals("Given exception returned unexpected handler class",
                BaseError.class, mutable.getExceptionHandlerByClass(
                        IndexOutOfBoundsException.class));
    }

    @Test
    public void populatedMutableConfiguration_clearRemovesAllContent() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        mutable.setRoute("", BaseTarget.class);
        mutable.setTargetRoute(BaseTarget.class, "");

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assert.assertFalse("Configuration should have routes.",
                mutable.isEmpty());
        Assert.assertTrue("Configuration should have exceptions.",
                mutable.hasExceptionTargets());

        mutable.clear();

        Assert.assertTrue("After clear all routes should have been removed.",
                mutable.isEmpty());
        Assert.assertTrue(
                "After clear all targetRoutes should have been removed. ",
                configuration.getTargetRoutes().isEmpty());
        Assert.assertFalse(
                "After clear all exception targets should have been removed.",
                mutable.hasExceptionTargets());
    }

    @Test
    public void twoTargetsRegisteredForPath_removingSingleTargetLeavesSecond() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        mutable.setRoute("", BaseTarget.class);
        mutable.setTargetRoute(BaseTarget.class, "");

        mutable.setRoute("", ParamTarget.class);
        mutable.setTargetRoute(ParamTarget.class, "");

        Assert.assertEquals(
                "Path '' with no parameters should return BaseTarget",
                BaseTarget.class,
                mutable.getRoute("", Collections.EMPTY_LIST).get());
        Assert.assertEquals(
                "Path '' with a String parameter should return ParamTarget",
                ParamTarget.class,
                mutable.getRoute("", Arrays.asList("parameter")).get());

        mutable.removeRoute("", BaseTarget.class);

        Assert.assertTrue("HasRoute for path '' should be available",
                mutable.hasRoute(""));
        Assert.assertFalse(
                "HasRoute for path '' with no parameters should return false",
                mutable.hasRoute("", Collections.EMPTY_LIST));
        Assert.assertTrue(
                "HasRoute for path '' with String parameter should return false",
                mutable.hasRoute("", Arrays.asList("parameter")));
    }

    @Test
    public void twoTargetsRegisteredForPath_removingBothRoutesWillRemovePath() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        mutable.setRoute("", BaseTarget.class);
        mutable.setTargetRoute(BaseTarget.class, "");

        mutable.getRouteTarget("").addRoute(ParamTarget.class);
        mutable.setTargetRoute(ParamTarget.class, "");

        Assert.assertEquals(
                "Path '' with no parameters should return BaseTarget",
                BaseTarget.class,
                mutable.getRoute("", Collections.EMPTY_LIST).get());
        Assert.assertEquals(
                "Path '' with a String parameter should return ParamTarget",
                ParamTarget.class,
                mutable.getRoute("", Arrays.asList("parameter")).get());

        Assert.assertTrue("HasRoute for path '' should be available",
                mutable.hasRoute(""));

        mutable.removeRoute("", BaseTarget.class);

        Assert.assertTrue(
                "HasRoute for path '' should be available after removing one of two targets",
                mutable.hasRoute(""));

        mutable.removeRoute("", ParamTarget.class);

        Assert.assertFalse("No route for path '' should exist",
                mutable.hasRoute(""));
    }

    @Test
    public void emptyConfiguration_allGetMethodsWork() {
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
        try {
            configuration.clear();
            Assert.fail(
                    "Configuration could be cleared even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }
        try {
            configuration.setRoute("home", BaseTarget.class);
            Assert.fail(
                    "Configuration could add a new target route even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration
                    .setRoute("home", BaseTarget.class);
            Assert.fail(
                    "Configuration could add a RouteTarget even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.setTargetRoute(BaseTarget.class, "home");
            Assert.fail(
                    "Configuration could add a RouteTarget even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.setErrorRoute(UnsupportedOperationException.class,
                    BaseTarget.class);
            Assert.fail(
                    "Configuration could add a exception handler even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.removeRoute(BaseTarget.class);
            Assert.fail(
                    "Configuration could remove route by class even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }

        try {
            configuration.removeRoute("home");
            Assert.fail(
                    "Configuration could remove route by string even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }
        try {
            configuration.removeRoute("home", BaseTarget.class);
            Assert.fail(
                    "Configuration could remove route by String,Class even if it should throw immutable state!");
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

    @Tag("div")
    public static class BaseError extends Component
            implements HasErrorParameter<IndexOutOfBoundsException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IndexOutOfBoundsException> parameter) {
            return 0;
        }
    }
}