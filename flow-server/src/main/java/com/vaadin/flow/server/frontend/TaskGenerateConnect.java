/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;

/**
 * Generate the Vaadin TS files for endpoints, and the Client API file.
 */
public interface TaskGenerateConnect extends FallibleCommand{

    /**
     * Initialize a task for generating TS files based.
     *
     * @param applicationProperties
     *            application properties file.
     * @param openApi
     *            openApi json file.
     * @param outputFolder
     *            the output folder.
     * @param frontendDirectory
     *            the frontend folder.
     */
    void init(File applicationProperties, File openApi,
                        File outputFolder, File frontendDirectory);

}
