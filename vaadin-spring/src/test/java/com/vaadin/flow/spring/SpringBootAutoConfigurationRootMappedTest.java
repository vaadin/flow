package com.vaadin.flow.spring;

import com.vaadin.flow.server.*;
import jakarta.servlet.ServletException;
import org.atmosphere.cpr.ApplicationConfig;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import java.util.Set;

@SpringBootTest(classes = SpringBootAutoConfiguration.class)
@ContextConfiguration(classes = SpringBootAutoConfigurationRootMappedTest.TestConfig.class)
public class SpringBootAutoConfigurationRootMappedTest {

    // private SpringBootAutoConfiguration autoConfiguration;
    @Autowired
    private ServletRegistrationBean<SpringServlet> servletRegistrationBean;
    @Autowired
    private Environment environment;
    @Autowired
    private MyFilter myFilter;

    @Test
    public void urlMappingPassedToAtmosphere() {
        Assert.assertTrue(RootMappedCondition.isRootMapping(RootMappedCondition.getUrlMapping(environment)));
        Assert.assertEquals(
                Set.of(VaadinServletConfiguration.VAADIN_SERVLET_MAPPING),
                servletRegistrationBean.getUrlMappings());
        Assert.assertEquals("/" + Constants.PUSH_MAPPING,
                servletRegistrationBean.getInitParameters()
                        .get(ApplicationConfig.JSR356_MAPPING_PATH));
    }

    @Test
    public void filtersAreRegisteredOnTheServlet() throws ServletException {
        SpringServlet servlet = servletRegistrationBean.getServlet();

        Assertions.assertEquals(1, servlet.getVaadinFilters().size(),
                "There should be 1 filter");
        Assertions.assertInstanceOf(MyFilter.class, servlet.getVaadinFilters().get(0),
                "MyFilter should be registered");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        MyFilter myFilter() {
            return new MyFilter();
        }
        }

    static class MyFilter implements VaadinFilter {

        @Override
        public void requestStart(VaadinRequest request,
                VaadinResponse response) {

        }

        @Override
        public void handleException(VaadinRequest request,
                VaadinResponse response, VaadinSession vaadinSession,
                Exception t) {
        }

        @Override
        public void requestEnd(VaadinRequest request, VaadinResponse response,
                VaadinSession session) {
        }
    }
}
