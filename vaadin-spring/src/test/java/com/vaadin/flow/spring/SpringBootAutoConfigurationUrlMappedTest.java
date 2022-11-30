package com.vaadin.flow.spring;

import java.util.Set;

import org.atmosphere.cpr.ApplicationConfig;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import com.vaadin.flow.server.Constants;

@SpringBootTest(classes = SpringBootAutoConfiguration.class)
@TestPropertySource(properties = { "vaadin.urlMapping = /zing/*" })
public class SpringBootAutoConfigurationUrlMappedTest {

    @Autowired
    private ServletRegistrationBean<SpringServlet> servletRegistrationBean;
    @Autowired
    private Environment environment;

    @Test
    public void urlMappingPassedToAtmosphere() {
        Assert.assertFalse(RootMappedCondition
                .isRootMapping(RootMappedCondition.getUrlMapping(environment)));
        Assert.assertEquals(Set.of("/zing/*"),
                servletRegistrationBean.getUrlMappings());
        Assert.assertEquals("/zing/" + Constants.PUSH_MAPPING,
                servletRegistrationBean.getInitParameters()
                        .get(ApplicationConfig.JSR356_MAPPING_PATH));
    }
}
