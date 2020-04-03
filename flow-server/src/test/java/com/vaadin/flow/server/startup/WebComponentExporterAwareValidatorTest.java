package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;

public class WebComponentExporterAwareValidatorTest {

    private static final String ERROR_HINT = "Move it to a single route/a top router layout/web component of the application";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private WebComponentExporterAwareValidator annotationValidator;
    private ServletContext servletContext;

    @Before
    public void init() {
        annotationValidator = new WebComponentExporterAwareValidator();
        servletContext = Mockito.mock(ServletContext.class);
    }

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @Push
    public static class ThemeViewportWithParent extends Component {
    }

    @Tag(Tag.DIV)
    @Push
    public static class NonRoutePush extends Component {
    }

    @Tag(Tag.DIV)
    @Push
    public static class WCExporter extends WebComponentExporter<Component> {

        public WCExporter() {
            super(Tag.DIV);
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }
    }

    @Tag(Tag.DIV)
    public static abstract class AbstractMain extends Component {
    }

    @Test
    public void process_no_exception_is_thrown_for_correctly_setup_classes()
            throws ServletException {
        annotationValidator
                .process(Stream.of(AbstractMain.class, WCExporter.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void process_all_failing_anotations_are_reported()
            throws ServletException {
        try {
            annotationValidator.process(
                    Collections.singleton(ThemeViewportWithParent.class),
                    servletContext);
            Assert.fail("No exception was thrown for faulty setup.");
        } catch (InvalidApplicationConfigurationException iace) {
            String errorMessage = iace.getMessage();
            assertHint(errorMessage, Push.class);
        }
    }

    @Test
    public void process_non_linked_push_throws() throws ServletException {
        assertNon_linked_theme_throws(NonRoutePush.class, Push.class);
    }

    private void assertNon_linked_theme_throws(Class<? extends Component> clazz,
            Class<? extends Annotation> annotationType)
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_HINT);
        expectedEx.expectMessage(String.format(
                "Class '%s' contains '%s', but it is not a router "
                        + "layout/top level route/web component.",
                clazz.getName(), "@" + annotationType.getSimpleName()));

        annotationValidator.process(
                Stream.of(clazz).collect(Collectors.toSet()), servletContext);
    }

    private void assertHint(String msg,
            Class<? extends Annotation> anntationType) {
        Assert.assertThat("Exception has hint.", msg,
                CoreMatchers.allOf(
                        CoreMatchers
                                .containsString(anntationType.getSimpleName()),
                        CoreMatchers.containsString(ERROR_HINT)));
    }
}