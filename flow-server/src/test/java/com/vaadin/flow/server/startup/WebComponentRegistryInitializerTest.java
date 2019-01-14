package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponent;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.webcomponent.WebComponentRegistry;

public class WebComponentRegistryInitializerTest {

    private WebComponentRegistryInitializer initializer;
    @Mock
    private WebComponentRegistry registry;
    @Mock
    private ServletContext servletContext;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        initializer = new WebComponentRegistryInitializer();
        Mockito.when(servletContext
                .getAttribute(WebComponentRegistry.class.getName()))
                .thenReturn(registry);
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void onStartUp() throws ServletException {
        initializer.onStartup(Stream.of(MyComponent.class, UserBox.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_noExceptionWithNullArguments() {
        try {
            initializer.onStartup(null, servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.onStartup should not throw with null argument");
        }
        // Expect a call to setWebComponents even if we have an empty or null set
        Mockito.verify(registry).setWebComponents(Collections.emptyMap());
    }

    @Test
    public void emptySet_noExceptionAndWebComponentsSet() {
        try {
            initializer.onStartup(Collections.emptySet(), servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "WebComponentRegistryInitializer.onStartup should not throw with empty set");
        }
        Mockito.verify(registry).setWebComponents(Collections.emptyMap());
    }

    @Test
    public void duplicateNamesFoundOnStartUp_exceptionIsThrown()
            throws ServletException {
        expectedEx.expect(IllegalArgumentException.class);
        initializer.onStartup(
                Stream.of(MyComponent.class, MyDuplicateComponent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void invalidCustomElementName_initializerThrowsException()
            throws ServletException {
        expectedEx.expect(InvalidCustomElementNameException.class);
        expectedEx.expectMessage(String.format(
                "WebComponent name '%s' for '%s' is not a valid custom element name.",
                "invalid", InvalidName.class.getCanonicalName()));

        initializer.onStartup(Collections.singleton(InvalidName.class),
                servletContext);
    }

    @WebComponent("my-component")
    public class MyComponent extends Component {
    }

    @WebComponent("my-component")
    public class MyDuplicateComponent extends Component {
    }

    @WebComponent("user-box")
    public class UserBox extends Component {
    }

    @WebComponent("invalid")
    public class InvalidName extends Component {
    }
}
