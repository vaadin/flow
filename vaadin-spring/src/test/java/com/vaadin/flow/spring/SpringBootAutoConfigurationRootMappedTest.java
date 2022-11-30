package com.vaadin.flow.spring;

import java.util.Set;

import org.atmosphere.cpr.ApplicationConfig;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.env.Environment;

import com.vaadin.flow.server.Constants;

@SpringBootTest(classes = SpringBootAutoConfiguration.class)
// @ContextConfiguration(SpringBootAutoConfiguration.class)
public class SpringBootAutoConfigurationRootMappedTest {

    // private SpringBootAutoConfiguration autoConfiguration;
    @Autowired
    private ServletRegistrationBean<SpringServlet> servletRegistrationBean;
    @Autowired
    private Environment environment;

    @Test
    public void urlMappingPassedToAtmosphere() {
        Assert.assertTrue(RootMappedCondition
                .isRootMapping(RootMappedCondition.getUrlMapping(environment)));
        Assert.assertEquals(
                Set.of(VaadinServletConfiguration.VAADIN_SERVLET_MAPPING),
                servletRegistrationBean.getUrlMappings());
        Assert.assertEquals("/" + Constants.PUSH_MAPPING,
                servletRegistrationBean.getInitParameters()
                        .get(ApplicationConfig.JSR356_MAPPING_PATH));
    }
}
