/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.startup;

import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.ERROR_MESSAGE_BEGINNING;
import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.NON_PARENT;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

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
            throws ServletException {
        try {
            annotationValidator.process(Stream
                    .of(InlineViewportWithParent.class,
                            BodySizeViewportWithParent.class,
                            ViewPortViewportWithParent.class)
                    .collect(Collectors.toSet()), servletContext);
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
            throws ServletException {
        expectedEx.expect(InvalidApplicationConfigurationException.class);
        expectedEx.expectMessage(ERROR_MESSAGE_BEGINNING + String.format(
                NON_PARENT, FailingMultiAnnotation.class.getName(),
                "@" + BodySize.class.getSimpleName() + ", " + "@"
                        + Inline.class.getSimpleName()));

        annotationValidator.process(Stream.of(FailingMultiAnnotation.class)
                .collect(Collectors.toSet()), servletContext);

        Assert.fail("No exception was thrown for faulty setup.");
    }

    @Test
    public void onStartUp_no_exception_is_thrown_for_correctly_setup_classes()
            throws ServletException {
        annotationValidator
                .process(Stream.of(MultiAnnotation.class, AbstractMain.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @HandlesTypes({ Viewport.class, BodySize.class, Inline.class })
    public static class HandlesTypesTest
            implements ServletContainerInitializer {

        @Override
        public void onStartup(Set<Class<?>> c, ServletContext ctx)
                throws ServletException {
        }
    }

    public static class ExtendedHandlesTypesTest extends HandlesTypesTest {

    }

    @HandlesTypes({ HasErrorParameter.class, HasSomethingElse.class })
    public class HasErrorParameterTest implements ServletContainerInitializer {
        @Override
        public void onStartup(Set<Class<?>> c, ServletContext ctx)
                throws ServletException {
        }
    }

    public interface HasSomethingElse {

    }

    public static class DummySomethingElse implements HasSomethingElse {
    }

    public static class DummyHasErrorParameter implements HasErrorParameter {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter parameter) {
            return 0;
        }

    }

    @Test
    public void selfReferencesAreRemoved() {
        HandlesTypesTest annotationTest = new HandlesTypesTest();
        assertTypes(annotationTest, Set.of(Viewport.class), Set.of());
        assertTypes(annotationTest,
                Set.of(Viewport.class, AnnotationValidatorTest.class),
                Set.of(AnnotationValidatorTest.class));
        assertTypes(annotationTest,
                Set.of(Viewport.class, AnnotationValidatorTest.class),
                Set.of(AnnotationValidatorTest.class));

        ExtendedHandlesTypesTest extendedAnnotationTest = new ExtendedHandlesTypesTest();
        assertTypes(extendedAnnotationTest, Set.of(Viewport.class), Set.of());

        HasErrorParameterTest interfaceTest = new HasErrorParameterTest();

        assertTypes(interfaceTest, Set.of(DummyHasErrorParameter.class),
                Set.of(DummyHasErrorParameter.class));
        assertTypes(interfaceTest, Set.of(DummyHasErrorParameter.class),
                Set.of(DummyHasErrorParameter.class));
        assertTypes(interfaceTest,
                Set.of(HasErrorParameter.class, DummyHasErrorParameter.class),
                Set.of(DummyHasErrorParameter.class));
        assertTypes(interfaceTest,
                Set.of(HasErrorParameter.class, HasSomethingElse.class,
                        DummyHasErrorParameter.class, DummySomethingElse.class),
                Set.of(DummyHasErrorParameter.class, DummySomethingElse.class));
    }

    private void assertTypes(Object testObject, Set<Class<?>> input,
            Set<Class<?>> expectedOutput) {
        Assert.assertEquals(expectedOutput, AbstractAnnotationValidator
                .removeHandleTypesSelfReferences(input, testObject));
    }
}
