/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jakarta.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.ResourceContentHash;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PWA(name = "foo", shortName = "bar")
class PwaRegistryTest {

    @PWA(name = "Custom Icon Path", shortName = "CIP", iconPath = "icons/splash/foo.png")
    private static class PwaWithCustomIconPath {
    }

    @PWA(name = "Custom Icon Path", shortName = "COP", offlinePath = "some/path.html")
    private static class PwaWithCustomOfflinePath {
    }

    @StyleSheet("app.css")
    @StyleSheet("./relative.css")
    @StyleSheet("/absolute.css")
    @StyleSheet("context://context.css")
    @StyleSheet("base://base.css")
    @StyleSheet("https://cdn.example.com/remote.css")
    @PWA(name = "App Shell PWA", shortName = "ASP")
    private static class PwaWithAppShellAndStyleSheet
            implements AppShellConfigurator {
    }

    /** The resources {@link PwaWithAppShellAndStyleSheet} refers to. */
    private static final Set<String> STYLESHEET_RESOURCES = Set.of("/app.css",
            "/relative.css", "/absolute.css", "/context.css", "/base.css");

    @StyleSheet("same.css")
    @StyleSheet("./same.css")
    @StyleSheet("context://same.css")
    @PWA(name = "Equivalent PWA", shortName = "EQP")
    private static class PwaWithEquivalentStyleSheets
            implements AppShellConfigurator {
    }

    @StyleSheet("it's.css")
    @PWA(name = "Quoted PWA", shortName = "QP")
    private static class PwaWithQuotedStyleSheet
            implements AppShellConfigurator {
    }

    private static List<PwaIcon> splashIconsForAppleDevices;

    @AfterEach
    void clearContentHashCache() {
        // Static and keyed by path only, so hashes computed here would
        // otherwise leak into other test classes using the same paths
        ResourceContentHash.clearCache();
    }

    @BeforeAll
    static void initPwaWithCustomIconPath() throws IOException {
        PwaRegistry registry = preparePwaRegistry(
                PwaWithCustomIconPath.class.getAnnotation(PWA.class));
        splashIconsForAppleDevices = registry.getIcons().stream().filter(
                icon -> "apple-touch-startup-image".equals(icon.getRel()))
                .collect(Collectors.toList());
    }

    @Test
    void pwaIconIsGeneratedBasedOnClasspathIcon_servletContextHasNoResources()
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
        assertEquals(26, stream.toByteArray()[36]);
    }

    private static PwaRegistry preparePwaRegistry(PWA pwa) throws IOException {
        return preparePwaRegistry(pwa, null, Set.of());
    }

    private static PwaRegistry preparePwaRegistry(PWA pwa,
            Class<? extends AppShellConfigurator> appShell,
            Set<String> resources) throws IOException {
        return withPwaRegistry(pwa, appShell, resources, "./", false,
                (registry, request) -> registry);
    }

    /**
     * Builds the runtime service worker JS for a request, the way
     * {@code PwaHandler} serves it.
     */
    private static String runtimeServiceWorkerJs(PWA pwa) throws IOException {
        return runtimeServiceWorkerJs(pwa, null, Set.of(), "./", false);
    }

    private static String runtimeServiceWorkerJs(PWA pwa,
            Class<? extends AppShellConfigurator> appShell,
            Set<String> resources) throws IOException {
        return runtimeServiceWorkerJs(pwa, appShell, resources, "./", false);
    }

    private static String runtimeServiceWorkerJs(PWA pwa,
            Class<? extends AppShellConfigurator> appShell,
            Set<String> resources, String contextRootRelativePath,
            boolean productionMode) throws IOException {
        return withPwaRegistry(pwa, appShell, resources,
                contextRootRelativePath, productionMode,
                PwaRegistry::getRuntimeServiceWorkerJs);
    }

    /**
     * Runs {@code action} against a registry built with mocked surroundings.
     * <p>
     * The action runs inside the {@code MockedStatic} scopes on purpose: the
     * stylesheet entries are built when the JS is requested rather than at
     * initialization, so calling the getter after this method returned would
     * see an unmocked {@code VaadinService.getCurrent()}.
     *
     * @param resources
     *            context-root-relative paths that exist in the simulated
     *            deployment, e.g. {@code /app.css}. Only these are reported as
     *            available and only these get a content hash, so anything else
     *            is treated as a missing resource.
     * @param contextRootRelativePath
     *            what the service reports for the request, i.e. the relative
     *            path from the servlet root to the context root
     * @param productionMode
     *            the deployment production mode, the single source
     *            {@code PwaRegistry} and {@code AppShellRegistry} both read
     */
    private static <T> T withPwaRegistry(PWA pwa,
            Class<? extends AppShellConfigurator> appShell,
            Set<String> resources, String contextRootRelativePath,
            boolean productionMode,
            BiFunction<PwaRegistry, VaadinRequest, T> action)
            throws IOException {
        try (MockedStatic<VaadinService> vaadinService = Mockito
                .mockStatic(VaadinService.class);
                MockedStatic<FeatureFlags> featureFlags = Mockito
                        .mockStatic(FeatureFlags.class)) {

            VaadinServletService vaadinServiceMocked = Mockito
                    .mock(VaadinServletService.class);
            Mockito.when(vaadinServiceMocked
                    .isResourceAvailable(Mockito.anyString()))
                    .thenAnswer((InvocationOnMock invocation) -> {
                        final String resourceUrl = (String) invocation
                                .getArguments()[0];
                        if (resources.contains(resourceUrl)) {
                            return true;
                        }
                        return invocation.callRealMethod();
                    });
            // Mimic ServiceContextUriResolver, which expands context:// to the
            // context root and base:// to the servlet root
            Mockito.when(
                    vaadinServiceMocked.resolveResource(Mockito.anyString()))
                    .thenAnswer(invocation -> {
                        String url = invocation.getArgument(0);
                        if (url.startsWith(
                                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)) {
                            return "/" + url.substring(
                                    ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                                            .length());
                        }
                        if (url.startsWith(
                                ApplicationConstants.BASE_PROTOCOL_PREFIX)) {
                            return url.substring(
                                    ApplicationConstants.BASE_PROTOCOL_PREFIX
                                            .length());
                        }
                        return url;
                    });
            // Only declared resources are readable, so ResourceContentHash
            // computes a hash for those and returns null for the rest
            Mockito.when(
                    vaadinServiceMocked.getStaticResource(Mockito.anyString()))
                    .thenAnswer(invocation -> resources
                            .contains(invocation.getArgument(0))
                                    ? PwaRegistryTest.class.getResource(
                                            "/META-INF/resources/icons/icon.png")
                                    : null);

            final Map<String, Object> attributeMap = new HashMap<>();
            ServletContext servletContext = Mockito.mock(ServletContext.class);
            Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                    .then(invocation -> attributeMap
                            .get(invocation.getArguments()[0].toString()));
            Mockito.doAnswer(invocation -> attributeMap.put(
                    invocation.getArguments()[0].toString(),
                    invocation.getArguments()[1])).when(servletContext)
                    .setAttribute(Mockito.anyString(), Mockito.any());

            final VaadinServletContext context = new VaadinServletContext(
                    servletContext);

            MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();
            deploymentConfiguration.setProductionMode(productionMode);
            Mockito.when(vaadinServiceMocked.getDeploymentConfiguration())
                    .thenReturn(deploymentConfiguration);

            vaadinService.when(VaadinService::getCurrent)
                    .thenReturn(vaadinServiceMocked);
            Mockito.when(vaadinServiceMocked.getContext()).thenReturn(context);

            FeatureFlags flags = Mockito.mock(FeatureFlags.class);
            featureFlags.when(() -> FeatureFlags.get(Mockito.any()))
                    .thenReturn(flags);

            AppShellRegistry.getInstance(context).setShell(appShell);

            // The request that fetches sw-runtime.js; its servlet root is what
            // the stylesheet entries are relative to
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.getService()).thenReturn(vaadinServiceMocked);
            Mockito.when(vaadinServiceMocked
                    .getContextRootRelativePath(Mockito.any()))
                    .thenReturn(contextRootRelativePath);

            return action.apply(new PwaRegistry(pwa, servletContext), request);
        }
    }

    @Test
    void pwaWithCustomBaseIconPath_splashScreenIconForAllSupportedAppleDevicesAndOrientationsAreGenerated() {
        assertEquals(26, splashIconsForAppleDevices.size());
    }

    @Test
    void pwaWithCustomBaseIconPath_splashScreenIconForAppleDevices_areGeneratedBasedOnIconPath() {
        boolean customBaseNameUsedInIconGeneration = splashIconsForAppleDevices
                .stream().allMatch(
                        icon -> icon.getHref().startsWith("icons/splash/foo"));
        assertTrue(customBaseNameUsedInIconGeneration);
    }

    @Test
    void pwaWithCustomBaseIconPath_splashScreenIconForIPadDevices_includeBothOrientations() {
        // iPad Pro 12.9
        Predicate<PwaIcon> iPadPro129 = icon -> (icon.getWidth() == 2048
                && icon.getHeight() == 2732)
                || (icon.getWidth() == 2732 && icon.getHeight() == 2048);
        List<String> mediaQueriesForIPadPro129 = splashIconsForAppleDevices
                .stream().filter(iPadPro129)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPadPro129.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPadPro129.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad Pro 11, 10.5
        Predicate<PwaIcon> iPadPro11And105 = icon -> (icon.getWidth() == 1668
                && icon.getHeight() == 2388)
                || (icon.getWidth() == 2388 && icon.getHeight() == 1668);
        List<String> mediaQueriesForIPadPro11And105 = splashIconsForAppleDevices
                .stream().filter(iPadPro11And105)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPadPro11And105.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPadPro11And105.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad Air 10.5
        Predicate<PwaIcon> iPadAir105 = icon -> (icon.getWidth() == 1668
                && icon.getHeight() == 2224)
                || (icon.getWidth() == 2224 && icon.getHeight() == 1668);
        List<String> mediaQueriesForIPadAir105 = splashIconsForAppleDevices
                .stream().filter(iPadAir105)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPadAir105.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPadAir105.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad 10.2
        Predicate<PwaIcon> iPad102 = icon -> (icon.getWidth() == 1620
                && icon.getHeight() == 2160)
                || (icon.getWidth() == 2160 && icon.getHeight() == 1620);
        List<String> mediaQueriesForIPad102 = splashIconsForAppleDevices
                .stream().filter(iPad102)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPad102.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPad102.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPad Pro 9.7, iPad Air 9.7, iPad 9.7, iPad mini 7.9
        Predicate<PwaIcon> iPad97And79 = icon -> (icon.getWidth() == 1536
                && icon.getHeight() == 2048)
                || (icon.getWidth() == 2048 && icon.getHeight() == 1536);
        List<String> mediaQueriesForIPad97And79 = splashIconsForAppleDevices
                .stream().filter(iPad97And79)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPad97And79.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPad97And79.stream()
                .filter(media -> media.contains("landscape")).count());
    }

    @Test
    void pwaWithCustomBaseIconPath_splashScreenIconForIPhoneDevices_includeBothOrientations() {
        // iPhone 13 Pro Max, iPhone 12 Pro Max
        Predicate<PwaIcon> iPhone13ProMaxAnd12ProMax = icon -> (icon
                .getWidth() == 1284 && icon.getHeight() == 2778)
                || (icon.getWidth() == 2778 && icon.getHeight() == 1284);
        List<String> mediaQueriesForIPhone13ProMaxAnd12ProMax = splashIconsForAppleDevices
                .stream().filter(iPhone13ProMaxAnd12ProMax)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone13ProMaxAnd12ProMax.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone13ProMaxAnd12ProMax.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 13 Pro, iPhone 13, iPhone 12 Pro, iPhone 12
        Predicate<PwaIcon> iPhone13ProAnd12ProAnd13And12 = icon -> (icon
                .getWidth() == 1170 && icon.getHeight() == 2532)
                || (icon.getWidth() == 2532 && icon.getHeight() == 1170);
        List<String> mediaQueriesForIPhone13ProAnd12ProAnd13And12 = splashIconsForAppleDevices
                .stream().filter(iPhone13ProAnd12ProAnd13And12)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone13ProAnd12ProAnd13And12.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone13ProAnd12ProAnd13And12.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 13 Mini, iPhone 12 Mini, iPhone 11 Pro, iPhone XS, iPhone X
        Predicate<PwaIcon> iPhone13MiniAnd12MiniAnd11ProAndXSAndX = icon -> (icon
                .getWidth() == 1125 && icon.getHeight() == 2436)
                || (icon.getWidth() == 2436 && icon.getHeight() == 1125);
        List<String> mediaQueriesForIPhone13MiniAnd12MiniAnd11ProAndXSAndX = splashIconsForAppleDevices
                .stream().filter(iPhone13MiniAnd12MiniAnd11ProAndXSAndX)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone13MiniAnd12MiniAnd11ProAndXSAndX
                .stream().filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone13MiniAnd12MiniAnd11ProAndXSAndX
                .stream().filter(media -> media.contains("landscape")).count());

        // iPhone 11 Pro Max, iPhone XS Max
        Predicate<PwaIcon> iPhone11ProMaxAndXSMax = icon -> (icon
                .getWidth() == 1242 && icon.getHeight() == 2688)
                || (icon.getWidth() == 2688 && icon.getHeight() == 1242);
        List<String> mediaQueriesForIPhone11ProMaxAndXSMax = splashIconsForAppleDevices
                .stream().filter(iPhone11ProMaxAndXSMax)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone11ProMaxAndXSMax.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone11ProMaxAndXSMax.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 11, iPhone XR
        Predicate<PwaIcon> iPhone11AndXR = icon -> (icon.getWidth() == 828
                && icon.getHeight() == 1792)
                || (icon.getWidth() == 1792 && icon.getHeight() == 828);
        List<String> mediaQueriesForIPhone11AndXR = splashIconsForAppleDevices
                .stream().filter(iPhone11AndXR)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone11AndXR.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone11AndXR.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus
        Predicate<PwaIcon> iPhone8PlusAnd7PlusAnd6sPlusAnd6Plus = icon -> (icon
                .getWidth() == 1242 && icon.getHeight() == 2208)
                || (icon.getWidth() == 2208 && icon.getHeight() == 1242);
        List<String> mediaQueriesForIPhone8PlusAnd7PlusAnd6sPlusAnd6Plus = splashIconsForAppleDevices
                .stream().filter(iPhone8PlusAnd7PlusAnd6sPlusAnd6Plus)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone8PlusAnd7PlusAnd6sPlusAnd6Plus
                .stream().filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone8PlusAnd7PlusAnd6sPlusAnd6Plus
                .stream().filter(media -> media.contains("landscape")).count());

        // iPhone 8, 7, 6s, 6, SE 4.7
        Predicate<PwaIcon> iPhone8And7And6sAnd6AndSE47 = icon -> (icon
                .getWidth() == 750 && icon.getHeight() == 1334)
                || (icon.getWidth() == 1334 && icon.getHeight() == 750);
        List<String> mediaQueriesForIPhone8And7And6sAnd6AndSE47 = splashIconsForAppleDevices
                .stream().filter(iPhone8And7And6sAnd6AndSE47)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone8And7And6sAnd6AndSE47.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone8And7And6sAnd6AndSE47.stream()
                .filter(media -> media.contains("landscape")).count());

        // iPhone 5, SE 4, iPod touch 5th Gen and later
        Predicate<PwaIcon> iPhone5AndSE47AndIPod5AndLater = icon -> (icon
                .getWidth() == 640 && icon.getHeight() == 1136)
                || (icon.getWidth() == 1136 && icon.getHeight() == 640);
        List<String> mediaQueriesForIPhone5AndSE47AndIPod5AndLater = splashIconsForAppleDevices
                .stream().filter(iPhone5AndSE47AndIPod5AndLater)
                .map(icon -> icon.asElement().attr("media"))
                .collect(Collectors.toList());
        assertEquals(1, mediaQueriesForIPhone5AndSE47AndIPod5AndLater.stream()
                .filter(media -> media.contains("portrait")).count());
        assertEquals(1, mediaQueriesForIPhone5AndSE47AndIPod5AndLater.stream()
                .filter(media -> media.contains("landscape")).count());
    }

    @Test
    void pwaWithCustomOfflinePath_getRuntimeServiceWorkerJsContainsCustomOfflinePath()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaWithCustomOfflinePath.class.getAnnotation(PWA.class));
        assertTrue(sw.contains("some/path.html"));
        assertFalse(sw.contains("{ url: '.', revision:"));
    }

    @Test
    void pwaWithoutCustomOfflinePath_getRuntimeServiceWorkerJsContainsCustomOfflinePath()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaRegistryTest.class.getAnnotation(PWA.class));
        assertTrue(sw.contains("{ url: '.', revision:"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void getRuntimeServiceWorkerJs_withoutRequest_omitsStyleSheets()
            throws IOException {
        // Without a request the stylesheet URLs cannot be resolved, so they
        // are left out rather than guessed
        String sw = withPwaRegistry(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, STYLESHEET_RESOURCES, "./",
                false,
                // NOSONAR deliberate: this test pins the contract of the
                // deprecated no-arg getter, which would otherwise be untested
                (registry, request) -> registry.getRuntimeServiceWorkerJs());
        assertTrue(sw.contains("self.additionalManifestEntries = ["));
        assertFalse(sw.contains("context.css"),
                "no stylesheet entry expected without a request, was: " + sw);
    }

    @Test
    void pwaWithAppShellAndStyleSheet_developmentMode_urlsAreServedNetworkFirst()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, STYLESHEET_RESOURCES);
        // The development href carries no content hash, so the same URL can
        // serve different contents over time. Precaching would pin the first
        // version fetched, so these go to the network-first list instead.
        assertFalse(sw.contains(
                "self.additionalManifestEntries = [\n{ url: './app.css'"),
                "stylesheets should not be precached in development, was: "
                        + sw);
        String networkFirst = networkFirstSection(sw);
        // AppShellRegistry skips adding Aura when app shell exists
        assertFalse(networkFirst.contains("aura/aura.css"));
        // With a root servlet mapping the servlet root is the context root,
        // so context:// expands to "./"
        assertTrue(networkFirst.contains("'./app.css'"));
        assertTrue(networkFirst.contains("'./relative.css'"));
        assertTrue(networkFirst.contains("'./context.css'"));
        // A leading '/' is already server-root-relative, so it is kept as is
        assertTrue(networkFirst.contains("'/absolute.css'"));
        // base:// stays relative, resolving against the service worker scope
        assertTrue(networkFirst.contains("'base.css'"));
        // External stylesheets are not handled at all
        assertFalse(sw.contains("cdn.example.com"));
    }

    @Test
    void pwaWithAppShellAndStyleSheet_nonRootServletMapping_contextUrlsStepUpToContextRoot()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, STYLESHEET_RESOURCES,
                "./../", false);
        // URLs stay relative and step up out of the servlet path, exactly
        // like the hrefs AppShellRegistry emits for the same stylesheets
        String networkFirst = networkFirstSection(sw);
        assertTrue(networkFirst.contains("'./../context.css'"),
                "expected context:// to step up to the context root, was: "
                        + sw);
        assertTrue(networkFirst.contains("'./../app.css'"));
        // base:// resolves against the service worker scope, which is the
        // servlet root, so it must stay relative without stepping up
        assertTrue(networkFirst.contains("'base.css'"));
    }

    @Test
    void pwaWithAppShellAndStyleSheet_productionMode_hasNoNetworkFirstUrls()
            throws IOException {
        // In production the URL carries the content hash, so it identifies one
        // version and cache-first precaching is correct
        String sw = runtimeServiceWorkerJs(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, STYLESHEET_RESOURCES, "./",
                true);
        assertFalse(sw.contains("additionalNetworkFirstUrls"),
                "production should precache instead, was: " + sw);
    }

    /**
     * Gets the {@code self.additionalNetworkFirstUrls} assignment, failing if
     * the service worker JS does not contain one.
     */
    private static String networkFirstSection(String sw) {
        int start = sw.indexOf("self.additionalNetworkFirstUrls = [");
        assertTrue(start >= 0, "expected a network-first URL list, was: " + sw);
        return sw.substring(start);
    }

    @Test
    void pwaWithAppShellAndStyleSheet_productionMode_urlsCarryContentHashParameter()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, STYLESHEET_RESOURCES, "./",
                true);
        // The <link href> carries ?v-c=<hash> in production, so the precache
        // entry has to carry it too to ever be matched
        Matcher matcher = Pattern
                .compile("\\{ url: './context\\.css\\?v-c=([0-9a-f]{8})', "
                        + "revision: '([0-9a-f]{8})' \\}")
                .matcher(sw);
        assertTrue(matcher.find(),
                "expected './context.css' entry with a ?v-c= parameter, was: "
                        + sw);
        assertEquals(matcher.group(1), matcher.group(2),
                "revision should be the same content hash as the parameter");
    }

    @Test
    void pwaWithAppShellAndStyleSheet_productionModeMissingResource_isNotPrecached()
            throws IOException {
        // Only context.css exists; an entry for a resource that cannot be read
        // would 404 and abort the whole service worker installation
        String sw = runtimeServiceWorkerJs(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, Set.of("/context.css"),
                "./", true);
        assertTrue(sw.contains("{ url: './context.css?v-c="));
        assertFalse(sw.contains("app.css"),
                "missing resource should not be precached, was: " + sw);
        assertFalse(sw.contains("absolute.css"));
    }

    @Test
    void pwaWithAppShellAndStyleSheet_developmentModeMissingResource_isStillServedNetworkFirst()
            throws IOException {
        // Network-first has no install step, so a fetch that fails is simply
        // not cached and nothing is aborted. There is therefore no reason to
        // leave an unreadable stylesheet out, unlike in production.
        String sw = runtimeServiceWorkerJs(
                PwaWithAppShellAndStyleSheet.class.getAnnotation(PWA.class),
                PwaWithAppShellAndStyleSheet.class, Set.of("/context.css"));
        String networkFirst = networkFirstSection(sw);
        assertTrue(networkFirst.contains("'./context.css'"));
        assertTrue(networkFirst.contains("'./app.css'"),
                "an unreadable stylesheet should still be listed, was: " + sw);
    }

    @Test
    void pwaWithAppShellAndEquivalentStyleSheets_isListedOnce()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaWithEquivalentStyleSheets.class.getAnnotation(PWA.class),
                PwaWithEquivalentStyleSheets.class, Set.of("/same.css"));
        assertEquals(1, countOccurrences(sw, "same.css"),
                "equivalent annotation values should yield one entry, was: "
                        + sw);
    }

    @Test
    void pwaWithAppShellAndQuotedStyleSheet_quoteIsEscapedInBothLists()
            throws IOException {
        // An unescaped quote would close the JS string literal and make
        // sw-runtime.js unparseable, breaking the whole service worker
        String devSw = runtimeServiceWorkerJs(
                PwaWithQuotedStyleSheet.class.getAnnotation(PWA.class),
                PwaWithQuotedStyleSheet.class, Set.of("/it's.css"));
        assertTrue(networkFirstSection(devSw).contains("'./it\\'s.css'"),
                "expected the quote to be escaped, was: " + devSw);

        String productionSw = runtimeServiceWorkerJs(
                PwaWithQuotedStyleSheet.class.getAnnotation(PWA.class),
                PwaWithQuotedStyleSheet.class, Set.of("/it's.css"), "./", true);
        assertTrue(productionSw.contains("{ url: './it\\'s.css?v-c="),
                "expected the quote to be escaped, was: " + productionSw);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }

    @Test
    void pwaWithoutAppShell_getRuntimeServiceWorkerJs_doesNotIncludeAuraCss()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaRegistryTest.class.getAnnotation(PWA.class), null, Set.of());
        assertFalse(sw.contains("aura/aura.css"));
    }

    @Test
    void pwaWithoutAppShell_AuraIsOnClassPath_getRuntimeServiceWorkerJs_includesAuraCss()
            throws IOException {
        String sw = runtimeServiceWorkerJs(
                PwaRegistryTest.class.getAnnotation(PWA.class), null,
                Set.of("/aura/aura.css"));
        assertTrue(networkFirstSection(sw).contains("'./aura/aura.css'"));
    }

}
