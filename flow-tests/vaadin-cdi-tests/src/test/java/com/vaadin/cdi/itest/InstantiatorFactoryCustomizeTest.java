/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.cdi.itest.instantiatorcustomize.InstantiatorAlternative;
import com.vaadin.cdi.itest.instantiatorcustomize.InstantiatorCustomizeView;
import com.vaadin.cdi.itest.instantiatorcustomize.InstantiatorFactoryAlternative;
import com.vaadin.cdi.itest.instantiatorcustomize.InstantiatorFactoryDecorator;

import static com.vaadin.cdi.itest.instantiatorcustomize.InstantiatorCustomizeView.CUSTOMIZED;
import static com.vaadin.cdi.itest.instantiatorcustomize.InstantiatorCustomizeView.VIEW;

@SuppressWarnings("ArquillianTooManyDeployment")
public class InstantiatorFactoryCustomizeTest extends AbstractCdiTest {

    @Deployment(name = "decorator", testable = false)
    public static WebArchive decoratorDeployment() {
        return ArchiveProvider.createWebArchive("instantiator-decorator-test",
                InstantiatorAlternative.class,
                InstantiatorFactoryDecorator.class,
                InstantiatorCustomizeView.class);
    }

    @Deployment(name = "alternative", testable = false)
    public static WebArchive alternativeDeployment() {
        return ArchiveProvider.createWebArchive("instantiator-alternative-test",
                InstantiatorAlternative.class,
                InstantiatorFactoryAlternative.class,
                InstantiatorCustomizeView.class);
    }

    @Before
    public void setUp() {
        open();
    }

    @Test
    @OperateOnDeployment("decorator")
    public void instantiatorCustomizedByDecorator() {
        assertInstantiatorCustomized();
    }

    @Test
    @OperateOnDeployment("alternative")
    public void instantiatorCustomizedByAlternative() {
        assertInstantiatorCustomized();
    }

    private void assertInstantiatorCustomized() {
        Assert.assertEquals(CUSTOMIZED, $("div").id(VIEW).getText());
    }

}
