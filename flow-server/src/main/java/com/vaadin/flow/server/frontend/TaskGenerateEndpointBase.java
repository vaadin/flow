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
 * Base interface for endpoint generation related tasks.
 */
public interface TaskGenerateEndpointBase extends FallibleCommand {
  
  /**
   * set application properties file.
   * @param applicationProperties
   *            the application properties file
   * @return the current task
   */
  TaskGenerateEndpointBase withApplicationProperties(File applicationProperties);

  /**
   * set the output folder.
   * @param outputFolder
   *            the output folder
   * @return the current task
   */
  TaskGenerateEndpointBase withOutputFolder(File outputFolder);

}
