/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.server;

import javax.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;
import org.slf4j.LoggerFactory;

@PWA(name = "foo", shortName = "bar")
public class PwaRegistryTest {

    @PWA(name = "Custom Icon Path", shortName = "CIP", iconPath = "icons/splash/foo.png")
    private static class PwaWithCustomIconPath {
    }

    private static List<PwaIcon> splashIconsForAppleDevices;

    @BeforeClass
    public static void initPwaWithCustomIconPath() throws IOException {
        PwaRegistry registry = preparePwaRegistry(
                PwaWithCustomIconPath.class.getAnnotation(PWA.class));
        splashIconsForAppleDevices = registry.getIcons().stream().filter(
                icon -> "apple-touch-startup-image".equals(icon.getRel()))
                .collect(Collectors.toList());
    }

    @Test
    public void pwaIconIsGeneratedBasedOnClasspathIcon_servletContextHasNoResources()
            throws IOException {
        // PWA annotation has default value for "iconPath" but servlet context
        // has no resource for that path, in that case the ClassPath URL will be
        // checked which is "META-INF/resources/icons/icon.png" (this path
        // available is in the test resources folder). The icon in this path
        // differs from the default icon and set of icons will be generated
        // based on it
        PwaRegistry registry = preparePwaRegistry(
                PwaRegistryTest.class.getAnnotation(PWA.class));
        List<PwaIcon> icons = registry.getIcons();
        // This icon has width 32 and it's generated based on a custom icon (see
        // above)
        PwaIcon pwaIcon = icons.stream().filter(icon -> icon.getWidth() == 32)
                .findFirst().get();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pwaIcon.write(stream);
        // the default image has 47 on the position 36
        Assert.assertEquals(26, stream.toByteArray()[36]);
    }

    private static PwaRegistry preparePwaRegistry(PWA pwa) throws IOException {
        try (MockedStatic<VaadinService> vaadinService = Mockito
                .mockStatic(VaadinService.class);
                MockedStatic<ApplicationConfiguration> configuration = Mockito
                        .mockStatic(ApplicationConfiguration.class)) {

            VaadinService vaadinServiceMocked = Mockito
                    .mock(VaadinService.class);
            VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
            ApplicationConfiguration applicationConfiguration = Mockito
                    .mock(ApplicationConfiguration.class);

            vaadinService.when(VaadinService::getCurrent)
                    .thenReturn(vaadinServiceMocked);
            Mockito.when(vaadinServiceMocked.getContext())
                    .thenReturn(vaadinContext);
            configuration
                    .when(() -> ApplicationConfiguration.get(Mockito.any()))
                    .thenReturn(applicationConfiguration);

            ServletContext context = Mockito.mock(ServletContext.class);
            return new PwaRegistry(pwa, context);
        }
    }

    @Test
    public void pwaWithCustomBaseIconPath_splashScreenIconForAllSupportedAppleDevicesAndOrientationsAreGenerated() {
        Assert.assertEquals(26, splashIconsForAppleDevices.size());
    }

    @Test
    public void pwaWithCustomBaseIconPath_splashScreenIconForAppleDevices_areGeneratedBasedOnIconPath() {
        boolean customBaseNameUsedInIconGeneration = splashIconsForAppleDevices
                .stream().allMatch(
                        icon -> icon.getHref().startsWith("icons/splash/foo"));
        Assert.assertTrue(customBaseNameUsedInIconGeneration);
    }

    @Test
    public void pwaWithCustomBaseIconPath_splashScreenIconForIPadDevices_includeBothOrientations() {
        // iPad Pro 12.9
        Predicate<PwaIcon> iPadPro129 = icon -> (icon.getWidth() == 2048
                && icon.getHeight() == 2732)
                || (icon.getWidth() == 2732 && icon.getHeight() == 2048);
        List<String> mediaQueriesForIPadPro129 = splashIconsForAppleDevices
                .stream().filter(iPadPro129)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPadPro129.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPadPro129.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad Pro 11, 10.5
        Predicate<PwaIcon> iPadPro11And105 = icon -> (icon.getWidth() == 1668
                && icon.getHeight() == 2388)
                || (icon.getWidth() == 2388 && icon.getHeight() == 1668);
        List<String> mediaQueriesForIPadPro11And105 = splashIconsForAppleDevices
                .stream().filter(iPadPro11And105)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPadPro11And105.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPadPro11And105.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad Air 10.5
        Predicate<PwaIcon> iPadAir105 = icon -> (icon.getWidth() == 1668
                && icon.getHeight() == 2224)
                || (icon.getWidth() == 2224 && icon.getHeight() == 1668);
        List<String> mediaQueriesForIPadAir105 = splashIconsForAppleDevices
                .stream().filter(iPadAir105)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPadAir105.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPadAir105.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad 10.2
        Predicate<PwaIcon> iPad102 = icon -> (icon.getWidth() == 1620
                && icon.getHeight() == 2160)
                || (icon.getWidth() == 2160 && icon.getHeight() == 1620);
        List<String> mediaQueriesForIPad102 = splashIconsForAppleDevices
                .stream().filter(iPad102)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPad102.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPad102.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad Pro 9.7, iPad Air 9.7, iPad 9.7, iPad mini 7.9
        Predicate<PwaIcon> iPad97And79 = icon -> (icon.getWidth() == 1536
                && icon.getHeight() == 2048)
                || (icon.getWidth() == 2048 && icon.getHeight() == 1536);
        List<String> mediaQueriesForIPad97And79 = splashIconsForAppleDevices
                .stream().filter(iPad97And79)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPad97And79.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPad97And79.stream()
                .filter(media -> media.contains("landscape")).count());
    }

    @Test
    public void pwaWithCustomBaseIconPath_splashScreenIconForIPhoneDevices_includeBothOrientations() {
        // iPhone 13 Pro Max, iPhone 12 Pro Max
        Predicate<PwaIcon> iPhone13ProMaxAnd12ProMax = icon -> (icon
                .getWidth() == 1284 && icon.getHeight() == 2778)
                || (icon.getWidth() == 2778 && icon.getHeight() == 1284);
        List<String> mediaQueriesForIPhone13ProMaxAnd12ProMax = splashIconsForAppleDevices
                .stream().filter(iPhone13ProMaxAnd12ProMax)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPhone13ProMaxAnd12ProMax.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPhone13ProMaxAnd12ProMax.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 13 Pro, iPhone 13, iPhone 12 Pro, iPhone 12
        Predicate<PwaIcon> iPhone13ProAnd12ProAnd13And12 = icon -> (icon
                .getWidth() == 1170 && icon.getHeight() == 2532)
                || (icon.getWidth() == 2532 && icon.getHeight() == 1170);
        List<String> mediaQueriesForIPhone13ProAnd12ProAnd13And12 = splashIconsForAppleDevices
                .stream().filter(iPhone13ProAnd12ProAnd13And12)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPhone13ProAnd12ProAnd13And12
                .stream().filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPhone13ProAnd12ProAnd13And12
                .stream().filter(media -> media.contains("landscape")).count());

        // iPhone 13 Mini, iPhone 12 Mini, iPhone 11 Pro, iPhone XS, iPhone X
        Predicate<PwaIcon> iPhone13MiniAnd12MiniAnd11ProAndXSAndX = icon -> (icon
                .getWidth() == 1125 && icon.getHeight() == 2436)
                || (icon.getWidth() == 2436 && icon.getHeight() == 1125);
        List<String> mediaQueriesForIPhone13MiniAnd12MiniAnd11ProAndXSAndX = splashIconsForAppleDevices
                .stream().filter(iPhone13MiniAnd12MiniAnd11ProAndXSAndX)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1,
                mediaQueriesForIPhone13MiniAnd12MiniAnd11ProAndXSAndX.stream()
                        .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1,
                mediaQueriesForIPhone13MiniAnd12MiniAnd11ProAndXSAndX.stream()
                        .filter(media -> media.contains("landscape")).count());

        // iPhone 11 Pro Max, iPhone XS Max
        Predicate<PwaIcon> iPhone11ProMaxAndXSMax = icon -> (icon
                .getWidth() == 1242 && icon.getHeight() == 2688)
                || (icon.getWidth() == 2688 && icon.getHeight() == 1242);
        List<String> mediaQueriesForIPhone11ProMaxAndXSMax = splashIconsForAppleDevices
                .stream().filter(iPhone11ProMaxAndXSMax)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPhone11ProMaxAndXSMax.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPhone11ProMaxAndXSMax.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 11, iPhone XR
        Predicate<PwaIcon> iPhone11AndXR = icon -> (icon.getWidth() == 828
                && icon.getHeight() == 1792)
                || (icon.getWidth() == 1792 && icon.getHeight() == 828);
        List<String> mediaQueriesForIPhone11AndXR = splashIconsForAppleDevices
                .stream().filter(iPhone11AndXR)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPhone11AndXR.stream()
                .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPhone11AndXR.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus
        Predicate<PwaIcon> iPhone8PlusAnd7PlusAnd6sPlusAnd6Plus = icon -> (icon
                .getWidth() == 1242 && icon.getHeight() == 2208)
                || (icon.getWidth() == 2208 && icon.getHeight() == 1242);
        List<String> mediaQueriesForIPhone8PlusAnd7PlusAnd6sPlusAnd6Plus = splashIconsForAppleDevices
                .stream().filter(iPhone8PlusAnd7PlusAnd6sPlusAnd6Plus)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1,
                mediaQueriesForIPhone8PlusAnd7PlusAnd6sPlusAnd6Plus.stream()
                        .filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1,
                mediaQueriesForIPhone8PlusAnd7PlusAnd6sPlusAnd6Plus.stream()
                        .filter(media -> media.contains("landscape")).count());

        // iPhone 8, 7, 6s, 6, SE 4.7
        Predicate<PwaIcon> iPhone8And7And6sAnd6AndSE47 = icon -> (icon
                .getWidth() == 750 && icon.getHeight() == 1334)
                || (icon.getWidth() == 1334 && icon.getHeight() == 750);
        List<String> mediaQueriesForIPhone8And7And6sAnd6AndSE47 = splashIconsForAppleDevices
                .stream().filter(iPhone8And7And6sAnd6AndSE47)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPhone8And7And6sAnd6AndSE47
                .stream().filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPhone8And7And6sAnd6AndSE47
                .stream().filter(media -> media.contains("landscape")).count());

        // iPhone 5, SE 4, iPod touch 5th Gen and later
        Predicate<PwaIcon> iPhone5AndSE47AndIPod5AndLater = icon -> (icon
                .getWidth() == 640 && icon.getHeight() == 1136)
                || (icon.getWidth() == 1136 && icon.getHeight() == 640);
        List<String> mediaQueriesForIPhone5AndSE47AndIPod5AndLater = splashIconsForAppleDevices
                .stream().filter(iPhone5AndSE47AndIPod5AndLater)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, mediaQueriesForIPhone5AndSE47AndIPod5AndLater
                .stream().filter(media -> media.contains("portrait")).count());
        Assert.assertEquals(1, mediaQueriesForIPhone5AndSE47AndIPod5AndLater
                .stream().filter(media -> media.contains("landscape")).count());
    }

}
