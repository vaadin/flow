package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.startup.AnnotationValidator.ERROR_MESSAGE_BEGINNING;
import static com.vaadin.flow.server.startup.AnnotationValidator.MIDDLE_ROUTER_LAYOUT;
import static com.vaadin.flow.server.startup.AnnotationValidator.NON_PARENT;
import static com.vaadin.flow.server.startup.AnnotationValidator.NON_PARENT_ALIAS;
import static com.vaadin.flow.server.startup.AnnotationValidator.NON_ROUTER_LAYOUT;

public class AnnotationValidatorTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AnnotationValidator annotationValidator;
    private ServletContext servletContext;

    @Before
    public void init() {
        annotationValidator = new AnnotationValidator();
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

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @Inline("inline.css")
    public static class InlineViewportWithParent extends Component {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @BodySize(width = "100vw")
    public static class BodySizeViewportWithParent extends Component {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class ViewPortViewportWithParent extends Component {
    }

    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class NonRoute extends Component {
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

    @Route("multiple_annotations")
    @Theme(MyTheme.class)
    @Inline("inlie.css")
    @BodySize(width = "100vw")
    @Viewport("width=device-width")
    public static class MultiAnnotation extends Component {
    }

    @Route(value = "multiple_annotations", layout = Parent.class)
    @Inline("inlie.css")
    @BodySize(width = "100vw")
    public static class FailingMultiAnnotation extends Component {
    }

    @Test
    public void onStartUp_route_can_not_contain_theme_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_MESSAGE_BEGINNING + String.format(
                NON_PARENT, ThemeViewportWithParent.class.getName(),
                Theme.class.getSimpleName()));

        annotationValidator.onStartup(Stream.of(ThemeViewportWithParent.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_all_failing_anotations_are_reported()
            throws ServletException {
        try {
            annotationValidator.onStartup(Stream
                    .of(ThemeViewportWithParent.class,
                            InlineViewportWithParent.class,
                            BodySizeViewportWithParent.class,
                            ViewPortViewportWithParent.class)
                    .collect(Collectors.toSet()), servletContext);
            Assert.fail("No exception was thrown for faulty setup.");
        } catch (InvalidApplicationConfigurationException iace) {
            String errorMessage = iace.getMessage();
            Assert.assertTrue("Exception has wrong beginning.",
                    errorMessage.startsWith(ERROR_MESSAGE_BEGINNING));
            Assert.assertTrue("Exception was missing Theme exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            ThemeViewportWithParent.class.getName(),
                            Theme.class.getSimpleName())));
            Assert.assertTrue("Exception was missing Inline exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            InlineViewportWithParent.class.getName(),
                            Inline.class.getSimpleName())));
            Assert.assertTrue("Exception was missing Viewport exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            ViewPortViewportWithParent.class.getName(),
                            Viewport.class.getSimpleName())));
            Assert.assertTrue("Exception was missing BodySize exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            BodySizeViewportWithParent.class.getName(),
                            BodySize.class.getSimpleName())));
        }
    }

    @Test
    public void onStartUp_route_can_not_contain_theme_if_alias_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_MESSAGE_BEGINNING + String.format(
                NON_PARENT_ALIAS, ThemeViewportWithAliasParent.class.getName(),
                Theme.class.getSimpleName()));

        annotationValidator
                .onStartup(Stream.of(ThemeViewportWithAliasParent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_non_linked_theme_throws() throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(
                ERROR_MESSAGE_BEGINNING + String.format(NON_ROUTER_LAYOUT,
                        NonRoute.class.getName(), Theme.class.getSimpleName()));

        annotationValidator.onStartup(
                Stream.of(NonRoute.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_middle_theme_throws() throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_MESSAGE_BEGINNING + String.format(
                MIDDLE_ROUTER_LAYOUT, MiddleThemeLayout.class.getName(),
                Theme.class.getSimpleName()));

        annotationValidator.onStartup(
                Stream.of(MiddleThemeLayout.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_all_failing_annotations_are_marked_for_class()
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_MESSAGE_BEGINNING + String.format(
                NON_PARENT, FailingMultiAnnotation.class.getName(),
                Inline.class.getSimpleName() + ", "
                        + BodySize.class.getSimpleName()));

        annotationValidator.onStartup(Stream.of(FailingMultiAnnotation.class)
                .collect(Collectors.toSet()), servletContext);

        Assert.fail("No exception was thrown for faulty setup.");
    }

    @Test
    public void onStartUp_no_exception_is_thrown_for_correctly_setup_classes()
            throws ServletException {
        annotationValidator.onStartup(
                Stream.of(ThemeRootViewport.class, ThemeRootParent.class,
                        MultiAnnotation.class, AbstractMain.class).collect(Collectors.toSet()),
                servletContext);
    }
}
