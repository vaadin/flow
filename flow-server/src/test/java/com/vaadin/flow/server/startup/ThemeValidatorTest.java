package com.vaadin.flow.server.startup;

import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.MIDDLE_ROUTER_LAYOUT;
import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.NON_PARENT;
import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.NON_PARENT_ALIAS;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

public class ThemeValidatorTest {

    private static final String THEME_ERROR_HINT = "Move it to a single route/a top router layout/web component of the application";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private ThemeValidator annotationValidator;
    private ServletContext servletContext;

    @Before
    public void init() {
        annotationValidator = new ThemeValidator();
        servletContext = Mockito.mock(ServletContext.class);
    }

    public static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/myTheme/";
        }
    }

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeViewportWithParent extends Component {
    }

    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class NonRoute extends Component {
    }

    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class WCExporter implements WebComponentExporter<Component> {

        @Override
        public void define(WebComponentDefinition<Component> definition) {
        }
    }

    @Route("root")
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeRootViewport extends Component {
    }

    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeRootParent extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    @ParentLayout(Parent.class)
    public static class MiddleThemeLayout extends Component
            implements RouterLayout {
    }

    @Route("")
    @RouteAlias(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeViewportWithAliasParent extends Component {
    }

    @Theme(MyTheme.class)
    @Tag(Tag.DIV)
    public static abstract class AbstractMain extends Component {
    }

    @Test
    public void onStartUp_no_exception_is_thrown_for_correctly_setup_classes()
            throws ServletException {
        annotationValidator
                .onStartup(Stream.of(AbstractMain.class, WCExporter.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_theme_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(THEME_ERROR_HINT);
        expectedEx.expectMessage(String.format(NON_PARENT,
                ThemeViewportWithParent.class.getName(),
                Theme.class.getSimpleName()));

        annotationValidator.onStartup(Stream.of(ThemeViewportWithParent.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_all_failing_anotations_are_reported()
            throws ServletException {
        try {
            annotationValidator.onStartup(
                    Collections.singleton(ThemeViewportWithParent.class),
                    servletContext);
            Assert.fail("No exception was thrown for faulty setup.");
        } catch (InvalidApplicationConfigurationException iace) {
            String errorMessage = iace.getMessage();
            assertHint(errorMessage);
            assertClassReport(errorMessage, ThemeViewportWithParent.class);
        }
    }

    @Test
    public void onStartUp_route_can_not_contain_theme_if_alias_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(THEME_ERROR_HINT);
        expectedEx.expectMessage(String.format(NON_PARENT_ALIAS,
                ThemeViewportWithAliasParent.class.getName(),
                Theme.class.getSimpleName()));

        annotationValidator
                .onStartup(Stream.of(ThemeViewportWithAliasParent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_non_linked_theme_throws() throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(THEME_ERROR_HINT);
        expectedEx.expectMessage(String.format(
                "Class '%s' contains '%s', but it is not a router "
                        + "layout/top level route/web component.",
                NonRoute.class.getName(), Theme.class.getName()));

        annotationValidator.onStartup(
                Stream.of(NonRoute.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_middle_theme_throws() throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(THEME_ERROR_HINT);
        expectedEx.expectMessage(String.format(MIDDLE_ROUTER_LAYOUT,
                MiddleThemeLayout.class.getName(),
                Theme.class.getSimpleName()));

        annotationValidator.onStartup(
                Stream.of(MiddleThemeLayout.class).collect(Collectors.toSet()),
                servletContext);
    }

    private void assertClassReport(String msg, Class<?> clazz) {
        Assert.assertThat("Exception was missing Theme exception", msg,
                CoreMatchers.containsString(String.format(NON_PARENT,
                        clazz.getName(), Theme.class.getSimpleName())));
    }

    private void assertHint(String msg) {
        Assert.assertThat("Exception has hint.", msg,
                CoreMatchers.allOf(
                        CoreMatchers
                                .containsString(Theme.class.getSimpleName()),
                        CoreMatchers.containsString(THEME_ERROR_HINT)));
    }
}
