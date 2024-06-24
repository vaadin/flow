/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

@PWA(name = "foo", shortName = "bar")
public class PwaRegistryTest {

    @PWA(name = "Custom Icon Path", shortName = "CIP", iconPath = "icons/splash/foo.png")
    private static class PwaWithCustomIconPath {
    }

    private static List<PwaIcon> splashIconsForAppleDevices;

    @BeforeClass
    public static void initPwaWithCustomIconPath() {
        ServletContext context = Mockito.mock(ServletContext.class);
        PwaRegistry registry = null;
        try {
            // Reflection is used here, because PwaRegistry has a private
            // constructor and the 'getInstance' method is really hard to
            // mocked up, it requires an access to
            // 'ApplicationRouteRegistryWrapper', which is protected and
            // invisible here.
            registry = createPwaRegistryInstance(
                    PwaWithCustomIconPath.class.getAnnotation(PWA.class),
                    context);
        } catch (Exception e) {
            Assert.fail("Failed to create an instance of PwaRegistry: "
                    + e.getMessage());
        }
        splashIconsForAppleDevices = registry.getIcons().stream().filter(
                icon -> "apple-touch-startup-image".equals(icon.getRel()))
                .collect(Collectors.toList());
    }

    @Test
    public void pwaIconIsGeneratedBasedOnClasspathIcon_servletContextHasNoResources() {
        ServletContext context = Mockito.mock(ServletContext.class);
        // PWA annotation has default value for "iconPath" but servlet context
        // has no resource for that path, in that case the ClassPath URL will be
        // checked which is "META-INF/resources/icons/icon.png" (this path
        // available is in the test resources folder). The icon in this path
        // differs from the default icon and set of icons will be generated
        // based on it
        PwaRegistry registry = null;
        try {
            // Reflection is used here, because PwaRegistry has a private
            // constructor and the 'getInstance' method is really hard to
            // mocked up, it requires an access to
            // 'ApplicationRouteRegistryWrapper', which is protected and
            // invisible here.
            registry = createPwaRegistryInstance(
                    PwaRegistryTest.class.getAnnotation(PWA.class), context);
        } catch (Exception e) {
            Assert.fail("Failed to create an instance of PwaRegistry: "
                    + e.getMessage());
        }

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

    private static PwaRegistry createPwaRegistryInstance(PWA pwa,
            ServletContext servletContext)
            throws IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchMethodException {
        Constructor<PwaRegistry> constructor = PwaRegistry.class
                .getDeclaredConstructor(PWA.class, ServletContext.class);
        constructor.setAccessible(true);
        return constructor.newInstance(pwa, servletContext);
    }

}
