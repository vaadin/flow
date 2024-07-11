/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.function;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.InitParameters;

public class DeploymentConfigurationTest {

    public abstract static class TestDeploymentConfiguration
            implements DeploymentConfiguration {

    }

    private TestDeploymentConfiguration configuration = Mockito
            .spy(TestDeploymentConfiguration.class);

    @Test
    public void useCompiledFrontendResources_notCompatibilityMode_productionMode_returnFalse() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Assert.assertFalse(configuration.useCompiledFrontendResources());
    }

    @Test
    public void licenseChecker_liveReloadDisabled_useOldLicenseChecker() {
        Mockito.when(configuration.isDevModeLiveReloadEnabled())
                .thenReturn(false);
        Assert.assertTrue(configuration.isOldLicenseCheckerEnabled());
    }

    @Test
    public void licenseChecker_configParameterIsTrue_useOldLicenseChecker() {
        Mockito.when(configuration.isDevModeLiveReloadEnabled())
                .thenReturn(true);
        Mockito.when(configuration.getBooleanProperty(Mockito.eq(
                InitParameters.SERVLET_PARAMETER_ENABLE_OLD_LICENSE_CHECKER),
                Mockito.eq(false))).thenReturn(true);
        Assert.assertTrue(configuration.isOldLicenseCheckerEnabled());
    }

    @Test
    public void licenseChecker_default_useNewLicenseChecker() {
        Assert.assertFalse(configuration.isOldLicenseCheckerEnabled());
    }

    @Test
    public void enforceFieldValidation_default_disabled() {
        Assert.assertFalse(configuration.isEnforcedFieldValidationEnabled());
    }

    @Test
    public void enforceFieldValidation_configParameterIsTrue_enabled() {
        Mockito.when(configuration.getBooleanProperty(
                Mockito.eq(InitParameters.ENFORCE_FIELD_VALIDATION),
                Mockito.eq(false))).thenReturn(true);
        Assert.assertTrue(configuration.isEnforcedFieldValidationEnabled());
    }
}
