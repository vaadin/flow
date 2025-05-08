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

package com.vaadin.flow.server.streams;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * File factory interface for generating file to store the uploaded data into.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface FileFactory extends Serializable {

    /**
     * Create a new file for given file name.
     *
     * @param fileName
     *            file name to create file for
     * @return {@link File} that should be used
     */
    File createFile(String fileName) throws IOException;
}