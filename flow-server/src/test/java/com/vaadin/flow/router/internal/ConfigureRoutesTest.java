package com.vaadin.flow.router.internal;

import java.util.Arrays;
import java.util.Collections;

import com.vaadin.flow.router.HasUrlParameterUtil;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;


public class ConfigureRoutesTest {

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

    @Test
    public void mutableConfiguration_canSetRouteTarget() {
        ConfigureRoutes mutable = new ConfigureRoutes();

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
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        Assert.assertTrue("", mutable.hasRouteTarget(BaseTarget.class));

        Assert.assertEquals("Configuration should have registered base target.",
                "", mutable.getTargetRoute(BaseTarget.class));
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        ConfiguredRoutes immutable = new ConfiguredRoutes(mutable);

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
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assert.assertFalse("Exception targets should be available",
                mutable.getExceptionHandlers().isEmpty());
        Assert.assertEquals("Given exception returned unexpected handler class",
                BaseError.class, mutable.getExceptionHandlerByClass(
                        IndexOutOfBoundsException.class));
    }

    @Test
    public void populatedMutableConfiguration_clearRemovesAllContent() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

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
                mutable.getTargetRoutes().isEmpty());
        Assert.assertFalse(
                "After clear  exception targets should still be available.",
                mutable.getExceptionHandlers().isEmpty());
    }

}