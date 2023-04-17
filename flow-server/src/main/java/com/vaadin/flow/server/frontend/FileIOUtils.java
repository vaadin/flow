/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileIOUtils {

    private FileIOUtils() {
        // Utils only
    }

    /**
     * Writes the given content into the given file unless the file already
     * contains that content.
     *
     * @param file
     *            the file to write to
     * @param content
     *            the lines to write
     * @return true if the content was written to the file, false otherwise
     * @throws IOException
     *             if something went wrong
     */
    public static boolean writeIfChanged(File file, List<String> content)
            throws IOException {
        return writeIfChanged(file,
                content.stream().collect(Collectors.joining("\n")));
    }

    /**
     * Writes the given content into the given file unless the file already
     * contains that content.
     *
     * @param file
     *            the file to write to
     * @param content
     *            the content to write
     * @return true if the content was written to the file, false otherwise
     * @throws IOException
     *             if something went wrong
     */
    public static boolean writeIfChanged(File file, String content)
            throws IOException {
        String existingFileContent = getExistingFileContent(file);
        if (content.equals(existingFileContent)) {
            // Do not write the same contents to avoid frontend recompiles
            log().debug("skipping writing to file '{}' because content matches",
                    file);
            return false;
        }

        log().debug("writing to file '{}' because content does not match",
                file);

        FileUtils.forceMkdirParent(file);
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        return true;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(FileIOUtils.class);
    }

    private static String getExistingFileContent(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

}
