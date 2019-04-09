package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.DevModeHandler;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS;
import static com.vaadin.flow.server.DevModeHandler.WEBPACK_CONFIG;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DevModeInitializerTest {

    private ServletContext servletContext;
    private DevModeInitializer devModeInitializer;
    private ServletRegistration registration;
    private Set<Class<?>> classes;

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() {
        servletContext = Mockito.mock(ServletContext.class);
        registration = Mockito.mock(ServletRegistration.class);
        classes = new HashSet<>();

        Map registry = new HashMap();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations()).thenReturn(registry);

        Mockito.when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        devModeInitializer = new DevModeInitializer();
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(new File("node_modules"));
        FileUtils.deleteQuietly(new File(PACKAGE_JSON));
        FileUtils.deleteQuietly(new File(WEBPACK_CONFIG));
        System.clearProperty("vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS);
    }

    @Test
    public void should_Not_Run_Updaters_when_Disabled() throws Exception {
        System.setProperty("vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS, "true");
        devModeInitializer.onStartup(classes, servletContext);
        assertFalse(new File(PACKAGE_JSON).canRead());
        assertFalse(new File(WEBPACK_CONFIG).canRead());
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_Enabled() throws Exception {
        devModeInitializer.onStartup(classes, servletContext);
        assertTrue(new File(PACKAGE_JSON).canRead());
        assertTrue(new File(WEBPACK_CONFIG).canRead());
        assertNotNull(DevModeHandler.getDevModeHandler());
    }
}
