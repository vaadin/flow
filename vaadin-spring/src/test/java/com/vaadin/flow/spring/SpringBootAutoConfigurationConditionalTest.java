package com.vaadin.flow.spring;

import com.vaadin.flow.spring.springnative.VaadinBeanFactoryInitializationAotProcessor;
import com.vaadin.testbench.unit.mocks.MockWebApplicationContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(SpringBootAutoConfigurationConditionalTest.class)
@WebAppConfiguration
@ContextConfiguration
public class SpringBootAutoConfigurationConditionalTest implements TestExecutionListener {

  private static final ThreadLocal<WebApplicationContext> WEB_APPLICATION_CONTEXT = new ThreadLocal<>();

  @Override
  public void beforeTestMethod(final TestContext testContext) throws Exception {
    WEB_APPLICATION_CONTEXT.set(new MockWebApplicationContext(testContext.getApplicationContext(), new MockServletContext()));
  }

  @Override
  public void afterTestMethod(final TestContext testContext) throws Exception {
    WEB_APPLICATION_CONTEXT.remove();
  }

  @Test
  public void customVaadinBeanFactoryInitializationAotProcessor() {
    new ApplicationContextRunner()
        .withBean(WebApplicationContext.class, WEB_APPLICATION_CONTEXT::get)
        .withUserConfiguration(VaadinBeanFactoryInitializationAotProcessorConfiguration.class, SpringBootAutoConfiguration.class)
        .run(context -> {
          assertThat(context)
              .getBean(VaadinBeanFactoryInitializationAotProcessor.class)
              .isInstanceOf(VaadinBeanFactoryInitializationAotProcessorConfiguration.MockVaadinBeanFactoryInitializationAotProcessor.class);
        });
  }

  @Test
  public void customServletContextInitializer() {
    new ApplicationContextRunner()
        .withBean(WebApplicationContext.class, WEB_APPLICATION_CONTEXT::get)
        .withUserConfiguration(ServletContextInitializerConfiguration.MockServletContextInitializer.class, SpringBootAutoConfiguration.class)
        .run(context -> {
          assertThat(context)
              .getBeans(ServletContextInitializer.class)
              .noneSatisfy((s, servletContextInitializer) -> {
                assertThat(servletContextInitializer)
                    .isOfAnyClassIn(VaadinServletContextInitializer.class);
              })
              .anySatisfy((s, servletContextInitializer) -> {
                assertThat(servletContextInitializer)
                    .isInstanceOf(ServletContextInitializerConfiguration.MockServletContextInitializer.class);
              });
        });
  }

  @Test
  public void customServletRegistrationBean() {
    new ApplicationContextRunner()
        .withBean(WebApplicationContext.class, WEB_APPLICATION_CONTEXT::get)
        .withUserConfiguration(ServletRegistrationBeanConfiguration.MockServletRegistrationBean.class, SpringBootAutoConfiguration.class)
        .run(context -> {
          assertThat(context)
              .getBean(ServletRegistrationBean.class)
              .isInstanceOf(ServletRegistrationBeanConfiguration.MockServletRegistrationBean.class);
        });
  }

  @Test
  public void customServerEndpointExporter() {
    new ApplicationContextRunner()
        .withBean(WebApplicationContext.class, WEB_APPLICATION_CONTEXT::get)
        .withUserConfiguration(ServerEndpointExporterConfiguration.MockServletEndpointExporter.class, SpringBootAutoConfiguration.class)
        .run(context -> {
          assertThat(context)
              .getBean(ServerEndpointExporter.class)
              .isInstanceOf(ServerEndpointExporterConfiguration.MockServletEndpointExporter.class);
        });
  }

  @Configuration(proxyBeanMethods = false)
  public static class VaadinBeanFactoryInitializationAotProcessorConfiguration {

    @Bean
    static VaadinBeanFactoryInitializationAotProcessor flowBeanFactoryInitializationAotProcessor() {
      return new MockVaadinBeanFactoryInitializationAotProcessor();
    }

    public static class MockVaadinBeanFactoryInitializationAotProcessor extends VaadinBeanFactoryInitializationAotProcessor {

    }

  }

  @Configuration(proxyBeanMethods = false)
  public static class ServletContextInitializerConfiguration {

    @Bean
    @Autowired
    public ServletContextInitializer contextInitializer(final WebApplicationContext webApplicationContext) {
      return new MockServletContextInitializer(webApplicationContext);
    }

    public static class MockServletContextInitializer extends VaadinServletContextInitializer {

      public MockServletContextInitializer(final ApplicationContext context) {
        super(context);
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  public static class ServletRegistrationBeanConfiguration {

    @Bean
    @Autowired
    public ServletRegistrationBean<SpringServlet> servletRegistrationBean(final WebApplicationContext webApplicationContext) {
      return new MockServletRegistrationBean(webApplicationContext);
    }

    public static class MockServletRegistrationBean extends ServletRegistrationBean<SpringServlet> {

      public MockServletRegistrationBean(final WebApplicationContext webApplicationContext) {
        super(new SpringServlet(webApplicationContext, false));
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  public static class ServerEndpointExporterConfiguration {

    @Bean
    public ServerEndpointExporter websocketEndpointExporter() {
      return new MockServletEndpointExporter();
    }

    public static class MockServletEndpointExporter extends VaadinWebsocketEndpointExporter {

    }

  }

}
