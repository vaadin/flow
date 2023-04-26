package com.vaadin.flow.server.frontend.scanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.FirstView;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.NoThemeExporter;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RootView2WithLayoutTheme;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RootViewWithLayoutTheme;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RootViewWithMultipleTheme;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RootViewWithTheme;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RootViewWithoutTheme;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.RootViewWithoutThemeAnnotation;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.SecondView;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Theme1;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Theme2;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Theme4;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.ThemeExporter;
import com.vaadin.flow.theme.AbstractTheme;

import static com.vaadin.flow.server.frontend.scanner.ScannerDependenciesTest.getFrontendDependencies;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScannerThemeTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void should_takeThemeFromTheView() {
        FrontendDependencies deps = getFrontendDependencies(
                RootViewWithTheme.class);

        assertEquals(Theme4.class, deps.getThemeDefinition().getTheme());

        DepsTests.assertImportsExcludingUI(deps.getModules(), "./theme-4.js");

        assertEquals(0, deps.getPackages().size());

        DepsTests.assertImports(deps.getScripts(), "frontend://theme-0.js");
    }

    @Test
    public void should_takeThemeFromLayout() {
        FrontendDependencies deps = getFrontendDependencies(
                RootViewWithLayoutTheme.class);
        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());
        assertEquals(Theme1.DARK, deps.getThemeDefinition().getVariant());

        DepsTests.assertImportCount(6, deps.getModules());
        assertEquals(1, deps.getPackages().size());
        DepsTests.assertImportCount(5, deps.getScripts());

        assertTrue(deps.getPackages().containsKey("@foo/first-view"));
        assertEquals("0.0.1", deps.getPackages().get("@foo/first-view"));
    }

    @Test
    public void should_takeTheme_when_AnyRouteValue() {
        FrontendDependencies deps = getFrontendDependencies(SecondView.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        DepsTests.assertImportCount(5, deps.getModules());
        assertEquals(0, deps.getPackages().size());
        DepsTests.assertImportCount(2, deps.getScripts());
    }

    @Test
    public void should_throw_when_MultipleThemes() {
        exception.expect(IllegalStateException.class);
        getFrontendDependencies(RootViewWithMultipleTheme.class,
                FirstView.class);
    }

    @Test
    public void should_throw_when_ThemeAndNoTheme() {
        exception.expect(IllegalStateException.class);
        getFrontendDependencies(FirstView.class, RootViewWithoutTheme.class);
    }

    @Test
    public void should_visitDefaultTheme_when_noThemeAnnotationIsGiven()
            throws Exception {

        DefaultClassFinder finder = spy(new DefaultClassFinder(
                Collections.singleton(RootViewWithoutThemeAnnotation.class)));

        // we'll do a partial mock here since we want to keep the other
        // behavior of the DefaultClassFinder. Theme4 is used as a fake Lumo
        // since it has @JsModule annotation which makes it easy to verify
        // that the Theme was actually visited and modules collected

        FrontendDependencies deps = new FrontendDependencies(finder) {
            @Override
            Class<? extends AbstractTheme> getDefaultTheme()
                    throws IOException {
                return Theme4.class;
            }
        };

        assertEquals(
                "Theme4 should have been returned when default theme was selected",
                Theme4.class, deps.getThemeDefinition().getTheme());
        DepsTests.assertImportsExcludingUI(deps.getModules(), "./theme-4.js");
    }

    @Test
    public void should_takeThemeFromExporter_when_exporterFound() {
        FrontendDependencies deps = getFrontendDependencies(
                ThemeExporter.class);

        assertEquals(Theme2.class, deps.getThemeDefinition().getTheme());
    }

    @Test
    public void should_defaultToLumoTheme_when_noThemeDefinedByExporter()
            throws Exception {
        // RootViewWithTheme is added to the list just to make sure exporter
        // handles theming default, not the other crawlers
        DefaultClassFinder finder = spy(new DefaultClassFinder(
                new HashSet<>(Arrays.asList(NoThemeExporter.class,
                        RootViewWithTheme.class))));

        Mockito.doReturn(Theme4.class).when(finder)
                .loadClass(FrontendDependencies.LUMO);

        FrontendDependencies deps = new FrontendDependencies(finder);
        assertEquals(
                "Theme4 should have been returned when default theme was selected",
                Theme4.class, deps.getThemeDefinition().getTheme());
        DepsTests.assertImportsExcludingUI(deps.getModules(), "./theme-4.js");
    }

    @Test // flow#5715
    public void should_notAttemptToOverrideTheme_when_noExportersFound()
            throws ClassNotFoundException {
        DefaultClassFinder finder = spy(new DefaultClassFinder(
                Collections.singleton(RootViewWithTheme.class)));

        new FrontendDependencies(finder);
        verify(finder, times(0)).loadClass(FrontendDependencies.LUMO);
    }

    @Test
    public void should_takeThemeFromLayout_ifLayoutAlreadyVisited() {
        // Make sure that all entry-points sharing layouts are correctly
        // theming-configured
        FrontendDependencies deps = getFrontendDependencies(
                RootViewWithLayoutTheme.class, RootView2WithLayoutTheme.class);
        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());
    }
}
