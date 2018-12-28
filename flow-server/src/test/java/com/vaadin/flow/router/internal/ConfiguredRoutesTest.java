package com.vaadin.flow.router.internal;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.startup.RouteTarget;

public class ConfiguredRoutesTest {

    @Test
    public void emptyConfiguration_allGetMethodsWork() {
        ConfiguredRoutes configuration = new ConfiguredRoutes();

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
        Assert.assertNull("No target route should be found", configuration
                .getTargetRoute(BaseTarget.class));
        Assert.assertTrue("Configuration should be empty",
                configuration.getExceptionHandlers().isEmpty());
        Assert.assertFalse("No route should be found", configuration
                .hasRouteTarget(BaseTarget.class));
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);
        mutable.setTargetRoute(BaseTarget.class, "");
        RouteTarget routeTarget = mutable.getRouteTarget("");
        routeTarget.setParentLayouts(BaseTarget.class,
                Arrays.asList(SecondParentTarget.class, ParentTarget.class));

        ConfiguredRoutes immutable = new ConfiguredRoutes(mutable);

        Assert.assertTrue("Configuration should have \"\" route registered",
                immutable.hasRoute(""));
        Assert.assertTrue("Exact match with no path segments should exist",
                immutable.hasRoute("", Collections.EMPTY_LIST));
        Assert.assertEquals("Configuration should have registered base target.",
                BaseTarget.class,
                immutable.getRoute("", Collections.EMPTY_LIST).get());

        Assert.assertTrue(
                "BaseTarget registration should have been copied over",
                immutable.hasRouteTarget(BaseTarget.class));
        Assert.assertEquals("Configuration should have registered base target.",
                "", immutable.getTargetRoute(BaseTarget.class));

        Assert.assertEquals(
                "Given parentLayouts should have been copied correctly",
                Arrays.asList(SecondParentTarget.class, ParentTarget.class),
                immutable.getParentLayouts("", BaseTarget.class));
    }

    @Tag("div")
    public static class BaseTarget extends Component {
    }

    @Tag("div")
    public static class ParentTarget extends Component implements RouterLayout {
    }

    @Tag("div")
    public static class SecondParentTarget extends Component
            implements RouterLayout {
    }
}
