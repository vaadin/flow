package com.vaadin.flow.router.internal;

import java.util.Arrays;
import java.util.EnumSet;

import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouteParameterRegex;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.RouterLayout;

public class ConfiguredRoutesTest {

    @Test
    public void emptyConfiguration_allGetMethodsWork() {
        ConfiguredRoutes configuration = new ConfiguredRoutes();

        Assert.assertFalse("No routes should be configured",
                configuration.hasTemplate(""));
        Assert.assertFalse("No routes should be configured",
                configuration.getTarget("")
                        .isPresent());
        Assert.assertTrue("Configuration should be empty",
                configuration.getRoutes().isEmpty());
        Assert.assertTrue("Configuration should be empty",
                configuration.getTargetRoutes().isEmpty());
        Assert.assertNull("No exception handler should be found.", configuration
                .getExceptionHandlerByClass(RuntimeException.class));
        Assert.assertNull("No target route should be found", configuration
                .getTemplate(BaseTarget.class));
        Assert.assertTrue("Configuration should be empty",
                configuration.getExceptionHandlers().isEmpty());
        Assert.assertFalse("No route should be found", configuration
                .hasRouteTarget(BaseTarget.class));
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class,
                Arrays.asList(SecondParentTarget.class, ParentTarget.class));

        ConfiguredRoutes immutable = new ConfiguredRoutes(mutable);

        Assert.assertTrue("Configuration should have \"\" route registered",
                immutable.hasTemplate(""));
        Assert.assertEquals("Configuration should have registered base target.",
                BaseTarget.class,
                immutable.getTarget("").get());

        Assert.assertTrue(
                "BaseTarget registration should have been copied over",
                immutable.hasRouteTarget(BaseTarget.class));
        Assert.assertEquals("Configuration should have registered base target.",
                "", immutable.getTemplate(BaseTarget.class));

        Assert.assertEquals(
                "Given parentLayouts should have been copied correctly",
                Arrays.asList(SecondParentTarget.class, ParentTarget.class),
                immutable.getParentLayouts("", BaseTarget.class));
    }

    @Test
    public void configuration_provides_target_url() {
        ConfigureRoutes edit = new ConfigureRoutes();
        edit.setRoute("foo/:foo", FooTarget.class);
        edit.setRoute("foo/:foo(qwe)", FooTarget.class);
        edit.setRoute("foo/:foo/bar?(asd)", FooTarget.class);
        edit.setRoute(":foo/:bar?(asd)", FooTarget.class);
        edit.setRoute("foo/:foo/:bar*(asd)", FooTarget.class);
        edit.setRoute("foo/:foo(qwe)/:bar*(asd)", FooTarget.class);
        edit.setRoute("foo/foobar?/:foo(qwe)/:bar*(asd)", FooTarget.class);
        edit.setRoute("foo/foobar?/:foo(qwe)", FooTarget.class);
        edit.setRoute("foo/foobar?/:foo", FooTarget.class);

        edit.setRoute("bar/:bar?", BarTarget.class);
        edit.setRoute("bar/:bar?/foobar", BarTarget.class);
        edit.setRoute("bar/:bar?(qwe)", BarTarget.class);
        edit.setRoute("bar/:bar?(qwe)/:foo*(asd)", BarTarget.class);
        edit.setRoute("bar/:bar?/:foo*", BarTarget.class);
        edit.setRoute(":bar?/:foo*", BarTarget.class);
        edit.setRoute(":bar?/foobar/:foo*", BarTarget.class);

        ConfiguredRoutes config = new ConfiguredRoutes(edit);

        Assert.assertNull(config.getTargetUrl(FooTarget.class));
        Assert.assertEquals("bar", config.getTargetUrl(BarTarget.class));

        // Make sure all routes are passed.
        config.getRouteModel().getRoutes().entrySet()
                .forEach(stringRouteTargetEntry -> {
                    final boolean requiredParameter = RouteFormat
                            .hasRequiredParameter(
                                    stringRouteTargetEntry.getKey());
                    
                    Assert.assertEquals(
                            stringRouteTargetEntry.getValue().getTarget()
                                    .equals(FooTarget.class),
                            requiredParameter);
                });
    }

    @Test
    public void configuration_provides_formatted_url_template() {
        ConfigureRoutes config = new ConfigureRoutes();

        final String template = "/path/to"
                + "/:intType(" + RouteParameterRegex.INTEGER + ")"
                + "/:longType?(" + RouteParameterRegex.LONG + ")"
                + "/:stringType?/:varargs*(thinking|of|U|and|I)";
        config.setRoute(template, BaseTarget.class);

        Assert.assertFalse("Template should not contain prefixed forward slash '/'",
                template.equals(config.getTemplate(BaseTarget.class)));

        Assert.assertEquals("Invalid template", template.substring(1),
                config.getTemplate(BaseTarget.class));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:intType(integer)/:longType?(long)/:stringType?(string)/:varargs*(string)",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.MODIFIER,
                                RouteParameterFormatOption.REGEX_NAME)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:intType/:longType?/:stringType?/:varargs*",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.MODIFIER)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:intType/:longType/:stringType/:varargs",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.NAME)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:(integer)/:?(long)/:?(string)/:*(string)",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.MODIFIER,
                                RouteParameterFormatOption.REGEX_NAME)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:(" + RouteParameterRegex.INTEGER + ")/:?("
                        + RouteParameterRegex.LONG
                        + ")/:?/:*(thinking|of|U|and|I)",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.MODIFIER,
                                RouteParameterFormatOption.REGEX)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:integer/:long/:string/:string",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.REGEX_NAME)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:/:?/:?/:*", config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.MODIFIER)));

        Assert.assertEquals("Invalid formatted template",
                "path/to/:" + RouteParameterRegex.INTEGER + "/:"
                        + RouteParameterRegex.LONG + "/:/:thinking|of|U|and|I",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.REGEX)));
    }

    @Tag("div")
    public static class FooTarget extends Component {
    }

    @Tag("div")
    public static class BarTarget extends Component {
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
