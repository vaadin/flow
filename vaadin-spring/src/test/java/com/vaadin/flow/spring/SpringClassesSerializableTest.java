/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.io.Serializable;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
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
                "com\\.vaadin\\.flow\\.spring\\.VaadinConfigurationProperties\\$Pnpm",
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
                "com\\.vaadin\\.flow\\.spring\\.VaadinApplicationConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.VaadinServletConfiguration\\$RootExcludeHandler",
                "com\\.vaadin\\.flow\\.spring\\.VaadinScopesConfig",
                "com\\.vaadin\\.flow\\.spring\\.VaadinSpringSecurity",
                "com\\.vaadin\\.flow\\.spring\\.SpringBootAutoConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.SpringSecurityAutoConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.SpringApplicationConfigurationFactory(\\$.*)?",
                "com\\.vaadin\\.flow\\.spring\\.SpringLookupInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.spring\\.VaadinConfigurationProperties",
                "com\\.vaadin\\.flow\\.spring\\.scopes\\.VaadinSessionScope",
                "com\\.vaadin\\.flow\\.spring\\.scopes\\.AbstractScope",
                "com\\.vaadin\\.flow\\.spring\\.scopes\\.VaadinUIScope",
                "com\\.vaadin\\.flow\\.spring\\.security\\.AuthenticationContext",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinAwareSecurityContextHolderStrategy",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinAwareSecurityContextHolderStrategyConfiguration",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinWebSecurity",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinWebSecurity\\$Http401UnauthorizedAccessDeniedHandler",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinWebSecurityConfigurerAdapter",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinWebSecurityConfigurerAdapter\\$Http401UnauthorizedAccessDeniedHandler",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinDefaultRequestCache",
                "com\\.vaadin\\.flow\\.spring\\.security\\.UidlRedirectStrategy",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinSavedRequestAwareAuthenticationSuccessHandler",
                "com\\.vaadin\\.flow\\.spring\\.security\\.VaadinSavedRequestAwareAuthenticationSuccessHandler\\$RedirectStrategy",
                "com\\.vaadin\\.flow\\.spring\\.security\\.stateless\\.JwtSecurityContextRepository",
                "com\\.vaadin\\.flow\\.spring\\.security\\.stateless\\.JwtSecurityContextRepository\\$UpdateJwtResponseWrapper",
                "com\\.vaadin\\.flow\\.spring\\.security\\.stateless\\.SerializedJwtSplitCookieRepository",
                "com\\.vaadin\\.flow\\.spring\\.security\\.stateless\\.VaadinStatelessSecurityConfigurer",
                "com\\.vaadin\\.flow\\.spring\\.security\\.stateless\\.VaadinStatelessSecurityConfigurer\\$SecretKeyConfigurer",
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
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(context.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(appConfig);
        Mockito.when(appConfig.getContext()).thenReturn(context);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        VaadinService service = new VaadinServletService(new VaadinServlet(),
                new DefaultDeploymentConfiguration(appConfig, getClass(),
                        initParameters)) {
            @Override
            public VaadinContext getContext() {
                return context;
            }
        };
        VaadinSession session = new TestSession(service);

        TestBeanStore store = new TestBeanStore(session);
        return store;
    }
}
