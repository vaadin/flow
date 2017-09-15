package com.vaadin.flow.router;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.ParentLayout;
import com.vaadin.annotations.Route;
import com.vaadin.ui.Component;

public class StaticRouteTargetRendererTest {

    @Test
    public void getRouterLayoutForSingle() throws Exception {
        StaticRouteTargetRenderer childRenderer = new StaticRouteTargetRenderer(
                RouteParentLayout.class);

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(Mockito.mock(NavigationEvent.class),
                        RouteParentLayout.class);

        Assert.assertEquals(
                "Found layout even though RouteParentLayout doesn't have any parents.",
                0, routerLayoutTypes.size());
    }

    @Test
    public void getRouterLayoutForSingleParent() throws Exception {
        StaticRouteTargetRenderer childRenderer = new StaticRouteTargetRenderer(
                SingleView.class);

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(Mockito.mock(NavigationEvent.class),
                        SingleView.class);

        Assert.assertEquals("Not all expected layouts were found", 1,
                routerLayoutTypes.size());
        Assert.assertEquals("Wrong class found", RouteParentLayout.class, routerLayoutTypes.get(0));
    }

    @Test
    public void getRouterLayoutForMulipleLayers() throws Exception {
        StaticRouteTargetRenderer childRenderer = new StaticRouteTargetRenderer(
                ChildConfiguration.class);

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(Mockito.mock(NavigationEvent.class),
                        ChildConfiguration.class);

        Assert.assertEquals("Not all expected layouts were found", 2,
                routerLayoutTypes.size());
        Assert.assertEquals("Wrong class found as first in array", MiddleLayout.class, routerLayoutTypes.get(0));
        Assert.assertEquals("Wrong class found as second in array", RouteParentLayout.class, routerLayoutTypes.get(1));
    }

    @Route("parent")
    private static class RouteParentLayout extends Component
            implements RouterLayout {
    }

    @ParentLayout(RouteParentLayout.class)
    private static class MiddleLayout extends Component
            implements RouterLayout {

    }

    @Route(value = "child", layout = MiddleLayout.class)
    private static class ChildConfiguration extends Component {
    }

    @Route(value = "single", layout = RouteParentLayout.class)
    private static class SingleView extends Component {
    }
}