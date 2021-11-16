package com.vaadin.flow.theme;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinSession;

public class ThemeUtilTest {

    public static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/myTheme/";
        }
    }

    public static class ThemeSingleNavigationTargetSubclass
            extends ThemeSingleNavigationTarget {
    }

    @Route("single")
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeSingleNavigationTarget extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @Theme(themeFolder = "my-theme")
    public static class ImprovedThemeSupport extends Component {
    }

    @Test
    public void navigationTargetWithTheme_subclassGetsTheme() {
        UI ui = mockUI(false);

        ThemeDefinition theme = ThemeUtil.findThemeForNavigationTarget(ui,
                ThemeSingleNavigationTargetSubclass.class, "single");
        Assert.assertNotNull(
                "Subclass should have a theme when the superclass has", theme);
        Assert.assertEquals(
                "Subclass should have the same theme as its superclass",
                MyTheme.class, theme.getTheme());
    }

    @Test
    public void navigationTargetWithImprovedThemeInCompatibilityMode_throwsException() {
        UI ui = mockUI(true);

        Assert.assertThrows(
                "themeFolder value in compatibilityMode should throw.",
                IllegalStateException.class,
                () -> ThemeUtil.findThemeForNavigationTarget(ui,
                        ImprovedThemeSupport.class, ""));
    }

    @Test
    public void navigationTargetWithImprovedThemeInNpmMode_getsTheme() {
        UI ui = mockUI(false);

        ThemeDefinition theme = ThemeUtil.findThemeForNavigationTarget(ui,
                ImprovedThemeSupport.class, "");

        Assert.assertNotNull("Theme should be gotten in npm mode", theme);
    }

    private UI mockUI(final boolean compatibilityMode) {
        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        Router router = new Router(registry);

        UIInternals uiInternals = Mockito.mock(UIInternals.class);
        Mockito.when(uiInternals.getRouter()).thenReturn(router);

        UI ui = Mockito.mock(UI.class);
        Mockito.when(ui.getInternals()).thenReturn(uiInternals);

        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(ui.getSession()).thenReturn(session);

        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isCompatibilityMode())
                .thenReturn(compatibilityMode);

        return ui;
    }

}
