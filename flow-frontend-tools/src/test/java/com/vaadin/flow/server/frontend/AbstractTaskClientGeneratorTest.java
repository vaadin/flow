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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class AbstractTaskClientGeneratorTest {

    private static final String TEST_STRING = "Hello world";

    @Test
    public void writeIfChanged_writesWithChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharsets.UTF_8);

        Assert.assertTrue(FileIOUtils.writeIfChanged(f, TEST_STRING + "2"));
    }

    @Test
    public void writeIfChanged_doesNotWriteWithoutChanges() throws Exception {
        File f = File.createTempFile("writeIfChanged", "aaa");
        FileUtils.write(f, TEST_STRING, StandardCharsets.UTF_8);
        Assert.assertFalse(FileIOUtils.writeIfChanged(f, TEST_STRING));
    }
}
