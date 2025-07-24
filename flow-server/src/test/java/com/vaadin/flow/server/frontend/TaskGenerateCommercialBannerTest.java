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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;

import static com.vaadin.flow.server.frontend.FrontendUtils.COMMERCIAL_BANNER_JS;

public class TaskGenerateCommercialBannerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File frontendFolder;
    private TaskGenerateCommercialBanner task;
    private Options options;

    @Before
    public void setUp() throws IOException {
        frontendFolder = temporaryFolder.newFolder();
        options = new Options(Mockito.mock(Lookup.class), null)
                .withFrontendDirectory(frontendFolder);
        options.withProductionMode(true);
        options.withBundleBuild(true);
        options.withCommercialBanner(true);
        task = new TaskGenerateCommercialBanner(options);
    }

    @Test
    public void execute_commercialBannerBuild_commercialBannerComponentGenerated()
            throws Exception {
        task.execute();
        Assert.assertTrue(
                "The generated file should exists only for bundle builds",
                task.getGeneratedFile().exists());
        Assert.assertEquals(
                "Should load correct default content from commercial-banner.js",
                IOUtils.toString(
                        getClass().getResourceAsStream(COMMERCIAL_BANNER_JS),
                        StandardCharsets.UTF_8),
                Files.readString(task.getGeneratedFile().toPath(),
                        StandardCharsets.UTF_8));
    }

    @Test
    public void execute_developmentMode_commercialBannerComponentNotGenerated()
            throws Exception {
        options.withProductionMode(false);
        task.execute();
        Assert.assertFalse(
                "The generated file should exists in development mode",
                task.getGeneratedFile().exists());
    }

    @Test
    public void execute_notBundleBuild_commercialBannerComponentNotGenerated()
            throws Exception {
        options.withBundleBuild(false);
        task.execute();
        Assert.assertFalse(
                "The generated file should exists only for bundle builds",
                task.getGeneratedFile().exists());
    }

    @Test
    public void execute_notCommercialBannerBuild_commercialBannerComponentNotGenerated()
            throws Exception {
        options.withCommercialBanner(false);
        task.execute();
        Assert.assertFalse(
                "The generated file should exists only for bundle builds",
                task.getGeneratedFile().exists());
    }

}
