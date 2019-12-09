/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import java.io.Serializable;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.scopes.TestBeanStore;
import com.vaadin.flow.testutil.ClassesSerializableTest;

public class SpringClassesSerializableTest extends ClassesSerializableTest {

    private static String CAPTURE;

    public static class TestSession extends VaadinSession {
        public TestSession(VaadinService service) {
            super(service);
        }

        @Override
        public boolean hasLock() {
            return true;
        }
    }

    private static class Callback implements Runnable, Serializable {

        @Override
        public void run() {
            CAPTURE = "bar";
        }

    }

    @Override
    protected Stream<String> getExcludedPatterns() {
        return Stream.concat(Stream.of(
                "com\\.vaadin\\.flow\\.spring\\.ForwardingRequestWrapper",
                "com\\.vaadin\\.flow\\.spring\\.VaadinScanPackagesRegistrar",
                "com\\.vaadin\\.flow\\.spring\\.VaadinScanPackagesRegistrar\\$VaadinScanPackages",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$AnnotationValidatorServletContextListener",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$ErrorParameterServletContextListener",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$DevModeServletContextListener",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$WebComponentServletContextListener",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$SpringStubServletConfig",
                "com\\.vaadin\\.flow\\.spring\\.VaadinMVCWebAppInitializer",
                "com\\.vaadin\\.flow\\.spring\\.RootMappedCondition",
                "com\\.vaadin\\.flow\\.spring\\.VaadinWebsocketEndpointExporter",
                "com\\.vaadin\\.flow\\.spring\\.DispatcherServletRegistrationBeanConfig",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.VaadinScopesConfig",
                "com\\.vaadin\\.flow\\.spring\\.SpringBootAutoConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.VaadinConfigurationProperties",
                "com\\.vaadin\\.flow\\.spring\\.scopes\\.VaadinSessionScope",
                "com\\.vaadin\\.flow\\.spring\\.scopes\\.AbstractScope",
                "com\\.vaadin\\.flow\\.spring\\.scopes\\.VaadinUIScope",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$ClassPathScanner",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletContextInitializer\\$CustomResourceLoader"),
                super.getExcludedPatterns());
    }

    @Before
    public void setUp() {
        CAPTURE = null;
    }

    @Test
    public void storeSerializableObject_objectIsRestoredAfterDeserialization()
            throws Throwable {
        TestBeanStore store = createStore();

        store.get("foo", () -> "bar");

        TestBeanStore deserialized = serializeAndDeserialize(store);

        Object object = deserialized.get("foo", () -> null);
        Assert.assertEquals("bar", object);
    }

    @Test
    public void storeSerializableCallback_callbackIsRestoredAfterDeserialization()
            throws Throwable {
        TestBeanStore store = createStore();

        Callback callback = new Callback();

        Assert.assertNull(CAPTURE);

        store.registerDestructionCallback("foo", callback);

        TestBeanStore deserialized = serializeAndDeserialize(store);

        deserialized.destroy();

        Assert.assertEquals("bar", CAPTURE);
    }

    private TestBeanStore createStore() {
        final Properties initParameters = new Properties();
        initParameters.setProperty(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.FALSE.toString());
        VaadinService service = new VaadinServletService(new VaadinServlet(),
                new DefaultDeploymentConfiguration(getClass(),
                        initParameters));
        VaadinSession session = new TestSession(service);

        TestBeanStore store = new TestBeanStore(session);
        return store;
    }
}
