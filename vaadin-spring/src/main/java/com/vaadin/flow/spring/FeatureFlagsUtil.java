package com.vaadin.flow.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import static com.vaadin.experimental.FeatureFlags.SYSTEM_PROPERTY_PREFIX;

/**
 * Purpose of this helper class is to provide universal access to
 * VAADINSERVLET_PUSH_MAPPING feature flag. This utility supports static access
 * and may be used even before Spring Component is initialized (during
 * SpringBootAutoconfiguration).
 */
@Component
public class FeatureFlagsUtil implements ApplicationContextAware {

    private static boolean servletMappingFeatureEnabled;

    /**
     * Returns if vaadinServletForPush feature is enabled considering both
     * FeatureFlags and Spring application property.
     *
     *
     */
    public static boolean isServletMappingFeatureEnabled() {
        if (VaadinService.getCurrent() != null
                && VaadinService.getCurrent().getContext() != null) {
            VaadinContext context = VaadinService.getCurrent().getContext();
            if (context.getAttribute(Lookup.class) != null) {
                return FeatureFlags.get(context)
                        .isEnabled(FeatureFlags.VAADINSERVLET_PUSH_MAPPING);
            }
        }

        return servletMappingFeatureEnabled;
    }

    public static boolean isServletMappingFeatureEnabled(
            ApplicationContext applicationContext) {
        servletMappingFeatureEnabled = loadApplicationProperty(
                applicationContext);
        return isServletMappingFeatureEnabled();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        servletMappingFeatureEnabled = loadApplicationProperty(
                applicationContext);
    }

    private static boolean loadApplicationProperty(
            ApplicationContext applicationContext) {
        String prop = applicationContext.getEnvironment()
                .getProperty(SYSTEM_PROPERTY_PREFIX
                        + FeatureFlags.VAADINSERVLET_PUSH_MAPPING.getId());

        return Boolean.parseBoolean(prop);
    }
}
