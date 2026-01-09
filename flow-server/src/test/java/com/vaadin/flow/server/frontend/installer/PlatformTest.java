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
package com.vaadin.flow.server.frontend.installer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.FrontendVersion;

import static com.vaadin.flow.server.frontend.installer.Platform.ALPINE_RELEASE_FILE_PATH;

public class PlatformTest {

    @Test
    public void testGuess_whenOsIsLinuxAndAlpineReleaseFileExists_unofficialNodeDownloadPathReturned() {
        try (MockedStatic<Platform.OS> os = Mockito
                .mockStatic(Platform.OS.class);
                MockedStatic<Paths> paths = Mockito.mockStatic(Paths.class);
                MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {

            os.when(Platform.OS::guess).thenReturn(Platform.OS.LINUX);
            Path alpineReleaseFilePath = Paths.get(ALPINE_RELEASE_FILE_PATH);
            paths.when(() -> Paths.get(ALPINE_RELEASE_FILE_PATH))
                    .thenReturn(alpineReleaseFilePath);
            files.when(() -> Files.exists(alpineReleaseFilePath))
                    .thenReturn(true);

            Platform platform = Platform.guess();
            Assert.assertEquals(NodeInstaller.UNOFFICIAL_NODEJS_DOWNLOAD_ROOT,
                    platform.getNodeDownloadRoot());

            FrontendVersion frontendVersion = Mockito
                    .mock(FrontendVersion.class);
            Assert.assertTrue(platform.getNodeClassifier(frontendVersion)
                    .contains("-musl"));
        }
    }

    @Test
    public void testGuess_whenOsIsLinuxAndAlpineReleaseFileDoesNotExist_officialNodeDownloadPathReturned() {
        try (MockedStatic<Platform.OS> os = Mockito
                .mockStatic(Platform.OS.class)) {

            os.when(Platform.OS::guess).thenReturn(Platform.OS.LINUX);

            Platform platform = Platform.guess();
            Assert.assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    platform.getNodeDownloadRoot());

            FrontendVersion frontendVersion = Mockito
                    .mock(FrontendVersion.class);
            Assert.assertFalse(platform.getNodeClassifier(frontendVersion)
                    .contains("-musl"));
        }
    }

    @Test
    public void testGuess_whenOsIsAnythingOtherThanLinuxAlpineRelease_officialNodeDownloadPathReturned() {
        try (MockedStatic<Platform.OS> os = Mockito
                .mockStatic(Platform.OS.class)) {

            os.when(Platform.OS::guess).thenReturn(Platform.OS.WINDOWS);

            Platform platform = Platform.guess();
            Assert.assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    platform.getNodeDownloadRoot());

            os.when(Platform.OS::guess).thenReturn(Platform.OS.MAC);

            platform = Platform.guess();
            Assert.assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    platform.getNodeDownloadRoot());

            os.when(Platform.OS::guess).thenReturn(Platform.OS.SUN_OS);

            platform = Platform.guess();
            Assert.assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    platform.getNodeDownloadRoot());
        }
    }
}
