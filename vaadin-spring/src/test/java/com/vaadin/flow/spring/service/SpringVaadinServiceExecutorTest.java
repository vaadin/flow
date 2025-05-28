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
 *
 */

package com.vaadin.flow.spring.service;

import java.util.Properties;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.annotation.VaadinTaskExecutor;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringVaadinServiceExecutorTest {

    static final String CUSTOM_EXECUTOR_VIA_INIT_LISTENER = "CUSTOM_EXECUTOR_VIA_INIT_LISTENER";
    static final String CUSTOM_NAMED_EXECUTOR = "CUSTOM_NAMED_EXECUTOR";
    static final String CUSTOM_EXECUTOR = "CUSTOM_EXECUTOR";
    static final String CUSTOM_SCHEDULER = "CUSTOM_SCHEDULER";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(SpringBootAutoConfiguration.class));

    @TestConfiguration
    static class CustomExecutorViaInitListenerConfig {
        @Bean
        VaadinServiceInitListener taskExecutorServiceInitListener() {
            return event -> event.setExecutor(
                    new TestTaskExecutor(CUSTOM_EXECUTOR_VIA_INIT_LISTENER));
        }
    }

    @TestConfiguration
    static class CustomExecutorBeanConfig {
        @Bean
        TaskExecutor myCustomTaskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }
    }

    @TestConfiguration
    static class CustomExecutorNamedBeanConfig {
        @Bean("VaadinTaskExecutor")
        TaskExecutor taskExecutor() {
            return new TestTaskExecutor(CUSTOM_NAMED_EXECUTOR);
        }
    }

    @TestConfiguration
    static class CustomExecutorAnnotatedBeanConfig {

        @Bean
        @VaadinTaskExecutor
        TaskExecutor taskExecutor() {
            return new TestTaskExecutor(CUSTOM_NAMED_EXECUTOR);
        }
    }

    @TestConfiguration
    @EnableAsync
    @Import(TaskExecutionAutoConfiguration.class)
    static class AsyncConfig {
    }

    @TestConfiguration
    @EnableAsync
    @EnableScheduling
    @Import({ TaskExecutionAutoConfiguration.class,
            TaskSchedulingAutoConfiguration.class })
    static class AsyncAndSchedulingConfig {
    }

    @TestConfiguration
    @EnableAsync
    @Import({ TaskExecutionAutoConfiguration.class })
    static class AsyncConfigWithCustomExecutorConfig {

        @Bean
        TaskExecutor myTaskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }
    }

    @TestConfiguration
    @EnableAsync
    @Import({ TaskExecutionAutoConfiguration.class })
    static class AsyncConfigWithNamedExecutorConfig {
        @Bean("VaadinTaskExecutor")
        TaskExecutor taskExecutor() {
            return new TestTaskExecutor(CUSTOM_NAMED_EXECUTOR);
        }
    }

    @TestConfiguration
    @EnableAsync
    @Import({ TaskExecutionAutoConfiguration.class })
    static class AsyncConfigWithAnnotatedExecutorConfig {
        @Bean("VaadinTaskExecutor")
        TaskExecutor taskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }
    }

    @TestConfiguration
    static class MultipleExecutorsConfig {
        @Bean
        TaskExecutor myCustomTaskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }

        @Bean
        TaskExecutor anotherCustomTaskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }
    }

    @TestConfiguration
    static class CustomExecutorAndSchedulerConfig {
        @Bean
        TaskExecutor myCustomTaskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }

        @Bean
        TaskScheduler myCustomTaskScheduler() {
            return new TestTaskScheduler(CUSTOM_SCHEDULER);
        }
    }

    @TestConfiguration
    static class CustomSchedulerConfig {
        @Bean
        TaskScheduler myCustomTaskScheduler() {
            return new TestTaskScheduler(CUSTOM_SCHEDULER);
        }
    }

    @TestConfiguration
    static class MultipleNamedExecutorsConfig {

        @VaadinTaskExecutor
        @Bean
        TaskExecutor taskExecutor2() {
            return new TestTaskExecutor("2");
        }

        @Bean("VaadinTaskExecutor1")
        TaskExecutor taskExecutor1() {
            return new TestTaskExecutor("1");
        }

        @VaadinTaskExecutor
        @Bean
        TaskExecutor taskExecutor3() {
            return new TestTaskExecutor("3");
        }
    }

    @TestConfiguration
    static class InvalidTypeAnnodatedConfig {

        @VaadinTaskExecutor
        @Bean
        Executor invalidTaskExecutor() {
            return command -> {
                throw new UnsupportedOperationException("BOOM!");
            };
        }

        @Bean
        TaskExecutor taskExecutor() {
            return new TestTaskExecutor(CUSTOM_EXECUTOR);
        }
    }

    @Test
    public void getExecutor_noSpringExecutor_returnsDefaultExecutor() {
        contextRunner.run(context -> {
            VaadinService service = SpringInstantiatorTest.getService(context,
                    new Properties());
            Executor executor = service.getExecutor();
            assertNotNull(executor,
                    "Should provide a default executor even if TaskExecutor bean is not defined");
            assertFalse(executor instanceof TaskExecutor,
                    "Should not return an instance of TaskExecutor");
        });
    }

    @Test
    public void getExecutor_springAsyncEnabled_returnsSpringDefaultExecutor() {
        contextRunner.withUserConfiguration(AsyncConfig.class).run(context -> {
            VaadinService service = SpringInstantiatorTest.getService(context,
                    new Properties());
            assertInstanceOf(TaskExecutor.class, service.getExecutor(),
                    "Expected a Spring TaskExecutor");
        });
    }

    @Test
    public void getExecutor_multipleSpringExecutors_returnsSpringDefaultApplicationTaskExecutor() {
        contextRunner.withUserConfiguration(AsyncAndSchedulingConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    assertInstanceOf(TaskExecutor.class, service.getExecutor(),
                            "Expected a Spring TaskExecutor");
                    assertFalse(service.getExecutor() instanceof TaskScheduler,
                            "Expected a Spring TaskExecutor, but got a TaskScheduler");
                });
    }

    @Test
    public void getExecutor_springDefaultAndCustomNamedExecutorBean_returnsCustomTaskExecutor() {
        contextRunner
                .withUserConfiguration(AsyncConfigWithNamedExecutorConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_NAMED_EXECUTOR, executor.name);
                });
    }

    @Test
    public void getExecutor_springDefaultAndCustomAnnotatedExecutorBean_returnsCustomTaskExecutor() {
        contextRunner
                .withUserConfiguration(
                        AsyncConfigWithAnnotatedExecutorConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_EXECUTOR, executor.name);
                });
    }

    @Test
    public void getExecutor_springDefaultAndCustomExecutorBean_returnsCustomTaskExecutor() {
        contextRunner
                .withUserConfiguration(
                        AsyncConfigWithCustomExecutorConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_EXECUTOR, executor.name);
                });
    }

    @Test
    public void getExecutor_customExecutorBean_returnsCustomTaskExecutor() {
        contextRunner.withUserConfiguration(CustomExecutorBeanConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_EXECUTOR, executor.name);
                });
    }

    @Test
    public void getExecutor_customSchedulerBean_returnsCustomTaskScheduler() {
        contextRunner.withUserConfiguration(CustomSchedulerConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskScheduler executor = assertInstanceOf(
                            TestTaskScheduler.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_SCHEDULER, executor.name);
                });
    }

    @Test
    public void getExecutor_customExecutorAndSchedulerBeans_returnsCustomTaskExecutor() {
        contextRunner
                .withUserConfiguration(CustomExecutorAndSchedulerConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_EXECUTOR, executor.name);
                });
    }

    @Test
    public void getExecutor_customExecutorNamedBean_returnsCustomTaskExecutorNamedInstance() {
        contextRunner.withUserConfiguration(CustomExecutorBeanConfig.class,
                CustomExecutorNamedBeanConfig.class).run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "VaadinService.getExecutor() should return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_NAMED_EXECUTOR, executor.name,
                            "Expected the named bean executor to be used");
                });
    }

    @Test
    public void getExecutor_customExecutorAnnotatedBean_returnsCustomTaskExecutorNamedInstance() {
        contextRunner
                .withUserConfiguration(CustomExecutorBeanConfig.class,
                        CustomExecutorAnnotatedBeanConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "VaadinService.getExecutor() should return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_NAMED_EXECUTOR, executor.name,
                            "Expected the named bean executor to be used");
                });
    }

    @Test
    public void getExecutor_asyncEnabledAndCustomExecutorNamedBean_returnsCustomTaskExecutorNamedInstance() {
        contextRunner.withUserConfiguration(AsyncConfig.class,
                CustomExecutorNamedBeanConfig.class).run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "VaadinService.getExecutor() should return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_NAMED_EXECUTOR, executor.name,
                            "Expected the named bean executor to be used");
                });
    }

    @Test
    public void getExecutor_customExecutorViaInitListener_returnsCustomTaskExecutor() {
        contextRunner
                .withUserConfiguration(
                        CustomExecutorViaInitListenerConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_EXECUTOR_VIA_INIT_LISTENER,
                            executor.name);
                });
    }

    @Test
    public void init_multipleUnnamedTaskExecutors_throws() {
        contextRunner.withUserConfiguration(MultipleExecutorsConfig.class)
                .run(context -> {
                    IllegalStateException error = assertThrows(
                            IllegalStateException.class,
                            () -> SpringInstantiatorTest.getService(context,
                                    new Properties()));
                    assertTrue(error.getMessage()
                            .contains("Multiple TaskExecutor beans found"));
                    assertTrue(error.getMessage()
                            .contains("@Bean(\"VaadinTaskExecutor\")"));
                    assertTrue(
                            error.getMessage().contains("@VaadinTaskExecutor"));
                });
    }

    @Test
    public void init_multipleNamedTaskExecutors_throws() {
        contextRunner.withUserConfiguration(MultipleNamedExecutorsConfig.class)
                .run(context -> {
                    IllegalStateException error = assertThrows(
                            IllegalStateException.class,
                            () -> SpringInstantiatorTest.getService(context,
                                    new Properties()));
                    assertTrue(error.getMessage()
                            .contains("Multiple TaskExecutor beans found"));
                    assertTrue(error.getMessage()
                            .contains("@Bean(\"VaadinTaskExecutor\")"));
                    assertTrue(
                            error.getMessage().contains("@VaadinTaskExecutor"));
                });
    }

    @Test
    public void getExecutor_invalidAnnotatedType_doesNotThrow() {
        contextRunner.withUserConfiguration(InvalidTypeAnnodatedConfig.class)
                .run(context -> {
                    VaadinService service = SpringInstantiatorTest
                            .getService(context, new Properties());
                    TestTaskExecutor executor = assertInstanceOf(
                            TestTaskExecutor.class, service.getExecutor(),
                            "Expected VaadinService.getExecutor() to return an instance of custom TaskExecutor");
                    assertEquals(CUSTOM_EXECUTOR, executor.name);
                });
    }

    /**
     * Simple TaskExecutor implementation for testing.
     */
    static class TestTaskExecutor implements TaskExecutor {

        final String name;

        public TestTaskExecutor(String name) {
            this.name = name;
        }

        @Override
        public void execute(Runnable task) {
            task.run();
        }
    }

    /**
     * Simple TaskScheduler implementation for testing.
     */
    static class TestTaskScheduler extends SimpleAsyncTaskScheduler {

        final String name;

        public TestTaskScheduler(String name) {
            this.name = name;
        }

    }
}
