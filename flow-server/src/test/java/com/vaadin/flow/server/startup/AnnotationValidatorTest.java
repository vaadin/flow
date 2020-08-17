package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;

import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.ERROR_MESSAGE_BEGINNING;
import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.NON_PARENT;

public class AnnotationValidatorTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AnnotationValidator annotationValidator;
    private VaadinContext vaadinContext;

    @Before
    public void init() {
        annotationValidator = new AnnotationValidator();
        vaadinContext = new VaadinServletContext(Mockito.mock(ServletContext.class));
    }

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
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
    public static abstract class AbstractMain extends Component {
    }

    @Route("multiple_annotations")
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
    public void onStartUp_all_failing_anotations_are_reported()
            throws VaadinInitializerException {
        try {
            annotationValidator.process(Stream
                    .of(InlineViewportWithParent.class,
                            BodySizeViewportWithParent.class,
                            ViewPortViewportWithParent.class)
                    .collect(Collectors.toSet()), vaadinContext);
            Assert.fail("No exception was thrown for faulty setup.");
        } catch (InvalidApplicationConfigurationException iace) {
            String errorMessage = iace.getMessage();
            Assert.assertTrue("Exception has wrong beginning.",
                    errorMessage.startsWith(ERROR_MESSAGE_BEGINNING));
            Assert.assertTrue("Exception was missing Inline exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            InlineViewportWithParent.class.getName(),
                            "@" + Inline.class.getSimpleName())));
            Assert.assertTrue("Exception was missing Viewport exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            ViewPortViewportWithParent.class.getName(),
                            "@" + Viewport.class.getSimpleName())));
            Assert.assertTrue("Exception was missing BodySize exception",
                    errorMessage.contains(String.format(NON_PARENT,
                            BodySizeViewportWithParent.class.getName(),
                            "@" + BodySize.class.getSimpleName())));
        }
    }

    @Test
    public void onStartUp_all_failing_annotations_are_marked_for_class()
            throws VaadinInitializerException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_MESSAGE_BEGINNING + String.format(
                NON_PARENT, FailingMultiAnnotation.class.getName(),
                "@" + BodySize.class.getSimpleName() + ", "
                        + "@" + Inline.class.getSimpleName()));

        annotationValidator.process(Stream.of(FailingMultiAnnotation.class)
                .collect(Collectors.toSet()), vaadinContext);

        Assert.fail("No exception was thrown for faulty setup.");
    }

    @Test
    public void onStartUp_no_exception_is_thrown_for_correctly_setup_classes()
            throws VaadinInitializerException {
        annotationValidator
                .process(Stream.of(MultiAnnotation.class, AbstractMain.class)
                        .collect(Collectors.toSet()), vaadinContext);
    }
}
