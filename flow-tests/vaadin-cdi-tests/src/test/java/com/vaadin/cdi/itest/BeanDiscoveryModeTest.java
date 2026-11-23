/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.cdi.itest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.cdi.itest.beandiscoverymode.BeanDiscoveryModeView;
import com.vaadin.cdi.itest.beandiscoverymode.CdiComponentGreetService;
import com.vaadin.cdi.itest.beandiscoverymode.NormalScopedGreetService;

import static com.vaadin.cdi.itest.beandiscoverymode.BeanDiscoveryModeView.CDI_SRV_BTN_ID;
import static com.vaadin.cdi.itest.beandiscoverymode.BeanDiscoveryModeView.CLEAR_NAME_BTN_ID;
import static com.vaadin.cdi.itest.beandiscoverymode.BeanDiscoveryModeView.NORMAL_SRV_BTN_ID;
import static com.vaadin.cdi.itest.beandiscoverymode.BeanDiscoveryModeView.RESULT_SPAN_ID;
import static com.vaadin.cdi.itest.beandiscoverymode.BeanDiscoveryModeView.SET_NAME_BTN_ID;

public class BeanDiscoveryModeTest extends AbstractCdiTest {

    private static final Class<?>[] CLASSES = { BeanDiscoveryModeView.class,
            CdiComponentGreetService.class, NormalScopedGreetService.class };

    @Deployment(name = "bean-discovery-mode-annotated", testable = false)
    public static WebArchive createImplicitBeanArchiveDeployment() {
        return ArchiveProvider
                .createWebArchive("bean-discovery-mode-annotated-test", CLASSES)
                .addAsWebInfResource(ArchiveProvider.class.getClassLoader()
                        .getResource("beans.xml"), "beans.xml");
    }

    @Deployment(name = "bean-discovery-mode-all", testable = false)
    public static WebArchive createCdiServletEnabledDeployment() {
        return ArchiveProvider.createWebArchive("bean-discovery-mode-all-test",
                CLASSES);
    }

    @Before
    public void setUp() {
        open();
    }

    @After
    public void cleanUp() {
        click(CLEAR_NAME_BTN_ID);
    }

    @Test
    @OperateOnDeployment("bean-discovery-mode-annotated")
    public void beanDiscoveryWorks_when_beanDiscoveryModeIsAnnotated() {
        validateBeanDiscoveryAndInjectionWorks();
    }

    @Test
    @OperateOnDeployment("bean-discovery-mode-all")
    public void beanDiscoveryWorks_when_beanDiscoveryModeIsAll() {
        validateBeanDiscoveryAndInjectionWorks();
    }

    private void validateBeanDiscoveryAndInjectionWorks() {
        click(SET_NAME_BTN_ID);
        click(NORMAL_SRV_BTN_ID);
        waitUntilNot(driver -> getText(RESULT_SPAN_ID) == null);
        Assert.assertEquals("Hello MyName from NormalScoped service.",
                getText(RESULT_SPAN_ID));

        cleanUp();

        click(SET_NAME_BTN_ID);
        click(CDI_SRV_BTN_ID);
        waitUntilNot(driver -> getText(RESULT_SPAN_ID) == null);
        Assert.assertEquals("Hello MyName from CDIComponent service.",
                getText(RESULT_SPAN_ID));
    }
}
