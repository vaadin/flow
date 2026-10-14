/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.service;

import jakarta.servlet.ServletException;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "vaadin.push-mode=MANUAL" })
@Import(TestServletConfiguration.class)
public class SpringServletTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void readUniformNameProperty_propertyNameContainsDash_propertyNameIsConvertedToCamelCaseAndReadProperly()
            throws ServletException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        PushMode pushMode = service.getDeploymentConfiguration().getPushMode();
        Assert.assertEquals(PushMode.MANUAL, pushMode);
    }

}
