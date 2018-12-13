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

        Assert.assertFalse("Exception targets should be available",
                mutable.getExceptionHandlers().isEmpty());
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
                mutable.getRoutes().isEmpty());
        Assert.assertFalse("Configuration should have exceptions.",
                mutable.getExceptionHandlers().isEmpty());

        mutable.clear();

        Assert.assertTrue("After clear all routes should have been removed.",
                mutable.getRoutes().isEmpty());
        Assert.assertTrue(
                "After clear all targetRoutes should have been removed. ",
                configuration.getTargetRoutes().isEmpty());
        Assert.assertFalse(
                "After clear  exception targets should still be available.",
                mutable.getExceptionHandlers().isEmpty());
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
    public void routesRegistered_getRouteTargetReturnsExpectedRouteTarget() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);
        mutable.setRoute("MyPath", BaseTarget.class);

        Assert.assertNotNull(
                "Expected a RouteTarget to be returned for 'MyPath'",
                mutable.getRouteTarget("MyPath"));

        mutable.setRoute("Another", BaseTarget.class);

        Assert.assertNotNull(
                "Expected a RouteTarget to be returned for 'Another'",
                mutable.getRouteTarget("Another"));

        mutable.setRoute("Another", ParamTarget.class);

        Assert.assertEquals("Expected single target for route 'MyPath'", 1,
                mutable.getRouteTarget("MyPath").getRoutes().size());
        Assert.assertEquals("Expected 2 targets for route 'Another'", 2,
                mutable.getRouteTarget("Another").getRoutes().size());
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
                configuration.getRoutes().isEmpty());
        Assert.assertTrue("Configuration should be empty",
                configuration.getTargetRoutes().isEmpty());
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
            configuration.setRoute("home", BaseTarget.class);
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
        try {
            configuration.copyFromTarget(new RouteConfiguration());
            Assert.fail(
                    "Configuration could copy data from another configuration even if it should throw immutable state!");
        } catch (IllegalStateException ise) {

        }
    }

    @Test
    public void copyFromTarget_contentsAreGottenCorrectly() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        mutable.setRoute("", BaseTarget.class);
        mutable.setTargetRoute(BaseTarget.class, "");

        mutable.setRoute("", ParamTarget.class);
        mutable.setTargetRoute(ParamTarget.class, "");

        RouteConfiguration copy = new RouteConfiguration(configuration, true);
        copy.clear();

        copy.copyFromTarget(mutable);

        Assert.assertEquals("Copy should have gotten one route path", 1,
                copy.getRoutes().size());
        Assert.assertEquals(
                "Copy should have gotten two routes for the single registered path",
                2, copy.getRouteTarget("").getRoutes().size());

        Assert.assertEquals(BaseTarget.class,
                copy.getRoute("", Collections.emptyList()).get());
        Assert.assertEquals(ParamTarget.class,
                copy.getRoute("", Collections.singletonList("single")).get());
    }

    @Test
    public void copyFromTarget_contentsAreAddedCorrectly() {
        RouteConfiguration mutable = new RouteConfiguration(configuration,
                true);

        mutable.setRoute("", BaseTarget.class);
        mutable.setTargetRoute(BaseTarget.class, "");

        mutable.setRoute("", ParamTarget.class);
        mutable.setTargetRoute(ParamTarget.class, "");

        RouteConfiguration copy = new RouteConfiguration(configuration, true);

        copy.setRoute("other", ParamTarget.class);
        copy.setTargetRoute(ParamTarget.class, "other");

        copy.copyFromTarget(mutable);

        Assert.assertEquals("Copy should have gotten one new route path", 2,
                copy.getRoutes().size());
        Assert.assertEquals(
                "Copy should have gotten two routes for the single registered path",
                2, copy.getRouteTarget("").getRoutes().size());

        Assert.assertEquals(BaseTarget.class,
                copy.getRoute("", Collections.emptyList()).get());
        Assert.assertEquals(ParamTarget.class,
                copy.getRoute("", Collections.singletonList("single")).get());

        Assert.assertEquals(ParamTarget.class,
                copy.getRoute("other", Collections.singletonList("single")).get());
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