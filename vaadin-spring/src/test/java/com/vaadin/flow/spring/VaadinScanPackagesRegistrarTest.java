package com.vaadin.flow.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.vaadin.flow.spring.annotation.EnableVaadin;

class VaadinScanPackagesRegistrarTest {

    @Test
    void multipleEnableVaadinConfigurations_shouldMergeScanPackages() {

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {

            context.register(ServiceAConfiguration.class,
                    ServiceBConfiguration.class);

            context.refresh();

            VaadinScanPackages scanPackages =
                    context.getBean(VaadinScanPackages.class);

            assertThat(scanPackages.getScanPackages())
                    .containsExactly(
                            "pkg.service.a.views",
                            "pkg.service.b.views");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableVaadin("pkg.service.a.views")
    static class ServiceAConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableVaadin("pkg.service.b.views")
    static class ServiceBConfiguration {
    }

    @Test
    void vaadinScanPackagesIsSerializable() throws Exception {
        VaadinScanPackages original =
                new VaadinScanPackages(
                        new String[] { "pkg.views" });

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ObjectOutputStream objectOutput =
                new ObjectOutputStream(output);

        objectOutput.writeObject(original);

        ObjectInputStream objectInput =
                new ObjectInputStream(
                        new ByteArrayInputStream(output.toByteArray()));

        VaadinScanPackages restored =
                (VaadinScanPackages) objectInput.readObject();

        assertThat(restored.getScanPackages())
                .containsExactly("pkg.views");
    }
}