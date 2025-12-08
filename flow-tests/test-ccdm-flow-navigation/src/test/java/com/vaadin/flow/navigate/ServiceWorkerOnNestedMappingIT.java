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
 */
package com.vaadin.flow.navigate;

import org.junit.After;
import org.junit.Ignore;

@Ignore("Service worker not working on nested path with VITE. See https://github.com/vaadin/flow/issues/14227")
public class ServiceWorkerOnNestedMappingIT extends ServiceWorkerIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/nested";
    }

    @After
    public void tearDown() {
        if (getDriver() != null) {
            checkLogsForErrors(message -> !message.toLowerCase()
                    .contains("failed to register a serviceworker"));
        }
    }

}
