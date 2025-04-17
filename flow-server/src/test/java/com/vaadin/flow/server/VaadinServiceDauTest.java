package com.vaadin.flow.server;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.dau.DAUCustomizer;
import com.vaadin.flow.server.dau.DAUVaadinRequestInterceptor;
import com.vaadin.flow.server.dau.UserIdentitySupplier;
import com.vaadin.pro.licensechecker.LicenseException;
import com.vaadin.pro.licensechecker.dau.DauIntegration;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.mockito.ArgumentMatchers.anyString;

public class VaadinServiceDauTest {

    private static final Predicate<VaadinRequestInterceptor> IS_DAU_INTERCEPTOR = interceptor -> interceptor instanceof DAUVaadinRequestInterceptor
            || (interceptor instanceof VaadinService.VaadinSessionOnRequestStartInterceptorWrapper wrapper
                    && wrapper.delegate instanceof DAUVaadinRequestInterceptor);

    private String subscriptionKey;
    private MockedStatic<DauIntegration> dauIntegrationMock;

    @Before
    public void setUp() throws Exception {
        subscriptionKey = System.getProperty("vaadin.subscriptionKey");
        System.setProperty("vaadin.subscriptionKey", "sub-1234");
        dauIntegrationMock = Mockito.mockStatic(DauIntegration.class);
    }

    @After
    public void tearDown() throws Exception {
        if (subscriptionKey != null) {
            System.setProperty("vaadin.subscriptionKey", subscriptionKey);
        } else {
            System.clearProperty("vaadin.subscriptionKey");
        }
        dauIntegrationMock.close();
    }

    @Test
    public void init_developmentMode_dauNotEnabled() {
        MockDeploymentConfiguration config = new MockDeploymentConfiguration();
        config.setProductionMode(false);
        config.setApplicationOrSystemProperty(Constants.DAU_TOKEN, "true");
        // VaadinService.init() is called in the constructor
        MockVaadinServletService service = new MockVaadinServletService(config);
        Assert.assertTrue("Expecting DAU interceptor not to be installed",
                vaadinInterceptors(service).noneMatch(IS_DAU_INTERCEPTOR));
        dauIntegrationMock.verifyNoInteractions();
    }

    @Test
    public void init_productionMode_notDauBuild_dauNotEnabled() {
        MockDeploymentConfiguration config = new MockDeploymentConfiguration();
        config.setProductionMode(true);
        config.setApplicationOrSystemProperty(Constants.DAU_TOKEN, "false");
        // VaadinService.init() is called in the constructor
        MockVaadinServletService service = new MockVaadinServletService(config);
        Assert.assertTrue("Expecting DAU interceptor not to be installed",
                vaadinInterceptors(service).noneMatch(IS_DAU_INTERCEPTOR));
        dauIntegrationMock.verifyNoInteractions();
    }

    @Test
    public void init_productionMode_dauBuild_dauEnabled() {
        MockDeploymentConfiguration config = new MockDeploymentConfiguration();
        config.setProductionMode(true);
        config.setApplicationOrSystemProperty(Constants.DAU_TOKEN, "true");
        // VaadinService.init() is called in the constructor
        MockVaadinServletService service = new MockVaadinServletService(config);
        Assert.assertTrue("Expecting DAU interceptor to be installed",
                vaadinInterceptors(service).anyMatch(IS_DAU_INTERCEPTOR));
        dauIntegrationMock
                .verify(() -> DauIntegration.startTracking(anyString()));
    }

    @Test
    public void init_productionMode_dauBuild_subscriptionKeyNotAvailable_throws() {
        dauIntegrationMock.reset();
        dauIntegrationMock.when(() -> DauIntegration.startTracking(anyString()))
                .thenCallRealMethod();
        System.clearProperty("vaadin.subscriptionKey");
        MockDeploymentConfiguration config = new MockDeploymentConfiguration();
        config.setProductionMode(true);
        config.setApplicationOrSystemProperty(Constants.DAU_TOKEN, "true");
        // VaadinService.init() is called in the constructor
        Assert.assertThrows(LicenseException.class,
                () -> new MockVaadinServletService(config));
    }

    @Test
    public void init_dauEnabled_lookupCustomIdentitySupplier() {

        UserIdentitySupplier providedIdentitySupplier = userIdentityContext -> Optional
                .of("user1");

        DAUCustomizer customizer = new DAUCustomizer() {
            @Override
            public UserIdentitySupplier getUserIdentitySupplier() {
                return providedIdentitySupplier;
            }
        };
        VaadinService service = vaadinServiceWithDau(customizer);

        VaadinRequestInterceptor interceptor = vaadinInterceptors(service)
                .filter(IS_DAU_INTERCEPTOR).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "DAU interceptor not installed"));
        // Ugly way to ensure custom user identity function in use
        UserIdentitySupplier userIdentitySupplier = extractUserIdentitySupplierFromDauInterceptor(
                interceptor);
        Assert.assertSame(providedIdentitySupplier, userIdentitySupplier);
    }

    public static VaadinService vaadinServiceWithDau(DAUCustomizer customizer) {
        MockDeploymentConfiguration config = new MockDeploymentConfiguration();
        config.setProductionMode(true);
        config.setApplicationOrSystemProperty(Constants.DAU_TOKEN, "true");
        Lookup lookup;
        if (customizer != null) {
            lookup = Lookup.of(customizer, DAUCustomizer.class);
        } else {
            lookup = null;
        }
        // VaadinService.init() is called in the constructor
        return new MockVaadinServletService(config) {

            @Override
            public VaadinContext getContext() {
                VaadinContext context = super.getContext();
                if (context.getAttribute(Lookup.class) == null) {
                    context.setAttribute(Lookup.class, lookup);
                }
                return context;
            }
        };
    }

    private Stream<VaadinRequestInterceptor> vaadinInterceptors(
            VaadinService service) {
        return StreamSupport.stream(
                service.getVaadinRequestInterceptors().spliterator(), false);
    }

    private UserIdentitySupplier extractUserIdentitySupplierFromDauInterceptor(
            VaadinRequestInterceptor interceptor) {
        if (interceptor instanceof VaadinService.VaadinSessionOnRequestStartInterceptorWrapper wrapper) {
            interceptor = wrapper.delegate;
        }
        if (interceptor instanceof DAUVaadinRequestInterceptor) {
            try {
                Field identitySupplierField = DAUVaadinRequestInterceptor.class
                        .getDeclaredField("userIdentitySupplier");
                identitySupplierField.setAccessible(true);
                return (UserIdentitySupplier) identitySupplierField
                        .get(interceptor);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AssertionError(
                        "Cannot access userIdentitySupplier field", e);
            }
        }
        throw new AssertionError(interceptor.getClass()
                + " is not a DAUVaadinRequestInterceptor nor a wrapped instance");
    }

}
