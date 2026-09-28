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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.cdi.itest.invaliddeployment.DeploymentView;
import com.vaadin.cdi.itest.invaliddeployment.NormalScopedLabel;

@RunWith(Arquillian.class)
@RunAsClient
public class InvalidDeploymentTest {

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "invalid-deployment", testable = false, managed = false)
    public static WebArchive invalidDeployment() {
        return ArchiveProvider.createWebArchive("invalid-deployment",
                DeploymentView.class, NormalScopedLabel.class);
    }

    @Test(expected = Exception.class)
    public void invalidDeploymentShouldBreakDeploy() {
        deployer.deploy("invalid-deployment");
    }

}
