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
package com.vaadin.viteapp;

import java.io.File;

import org.junit.Test;

import com.vaadin.flow.internal.FrontendUtils;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that a production build gets the Flow client without the project
 * depending on it. This module has no dependency on the client, the way an
 * application has none, so the build has to resolve it for the bundle to have
 * the client at all.
 */
public class FlowClientIT {

    @Test
    public void clientIsResolvedForTheFrontendBuild() {
        File jarResources = new File(System.getProperty("user.dir", "."),
                FrontendUtils.DEFAULT_FRONTEND_DIR + FrontendUtils.GENERATED
                        + FrontendUtils.JAR_RESOURCES_FOLDER);
        File client = new File(jarResources,
                "internal/client/bootstrap/Bootstrapper.js");

        assertTrue("The frontend build should have copied the Flow client "
                + "into " + jarResources + ", even though the project does "
                + "not depend on it", client.exists());
    }

}
