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
package com.vaadin.flow.quarkus.test;

import java.util.List;
import java.util.Set;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

public class VaadinServiceEnabledBeanUnremovableTest {

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addClasses(TestConfig.class, CustomI18NProvider.class)
                    .add(EmptyAsset.INSTANCE, "META-INF/beans.xml"));

    @Inject
    BeanManager beanManager;

    /*
     * I18nProvider is looked up only programmatically, so Quarkus will remove
     * the bean provided by TestConfig, unless it is marked @Unremovable. The
     * Vaadin extension makes sure that all @VaadinServiceEnabled beans are
     * automatically made unremovable, so the user does not need to apply
     * the @Unremovable annotation manually.
     */
    @Test
    void vaadinServiceEnabledBean_markedUnremovableByExtension() {
        Set<Bean<?>> candidates = beanManager.getBeans(I18NProvider.class,
                VaadinServiceEnabled.Literal.INSTANCE);
        Assertions.assertEquals(1, candidates.size());

        Bean<?> bean = candidates.iterator().next();
        CreationalContext<?> context = beanManager
                .createCreationalContext(bean);
        I18NProvider injected = (I18NProvider) beanManager.getReference(bean,
                I18NProvider.class, context);
        Assertions.assertInstanceOf(CustomI18NProvider.class, injected);
    }

    private static class TestConfig {

        @Produces
        @VaadinServiceEnabled
        @Singleton
        I18NProvider customI18nProvider() {
            return new CustomI18NProvider();
        }
    }

    private static class CustomI18NProvider extends DefaultI18NProvider {

        public CustomI18NProvider() {
            super(List.of());
        }
    }
}
