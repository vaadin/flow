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
public interface TaskGenerateConnect extends TaskGenerateEndpointBase {

  /**
   * set the openApi json file.
   * @param openApi
   *            the openApi json file
   * @return the current task
   */
  TaskGenerateConnect withOpenApi(File openApi);

  /**
   * set the frontend directory.
   * @param frontendDirectory
   *            the frontend directory
   * @return the current task
   */
  TaskGenerateConnect withFrontendDirectory(File frontendDirectory);
}
