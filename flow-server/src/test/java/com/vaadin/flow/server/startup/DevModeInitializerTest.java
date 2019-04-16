package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.DevModeHandler;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DevModeInitializerTest {
    private ServletContext servletContext;
    private DevModeInitializer devModeInitializer;
    private Set<Class<?>> classes;

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() {
        new File(tmpDir.getRoot(), "src").mkdir();
        System.setProperty("user.dir", tmpDir.getRoot().getPath());

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration registration = Mockito.mock(ServletRegistration.class);
        classes = new HashSet<>();

        Map registry = new HashMap();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations()).thenReturn(registry);
        Mockito.when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        devModeInitializer = new DevModeInitializer();
    }

    @After
    public void teardown() {
        System.clearProperty("vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS);
    }

    @Test
    public void should_Not_Run_Updaters_when_Disabled() throws Exception {
        System.setProperty("vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM, "true");
        System.setProperty("vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS, "true");
        devModeInitializer.onStartup(classes, servletContext);
        assertFalse(new File(tmpDir.getRoot(), PACKAGE_JSON).canRead());
        assertFalse(new File(tmpDir.getRoot(), WEBPACK_CONFIG).canRead());
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_Enabled() throws Exception {
        devModeInitializer.onStartup(classes, servletContext);
        System.err.println(tmpDir + " " + PACKAGE_JSON);

        System.err.println(new File(tmpDir.getRoot(), PACKAGE_JSON).getCanonicalPath());
        assertTrue(new File(tmpDir.getRoot(), PACKAGE_JSON).canRead());
        assertTrue(new File(tmpDir.getRoot(), WEBPACK_CONFIG).canRead());
        assertNotNull(DevModeHandler.getDevModeHandler());
    }
}
