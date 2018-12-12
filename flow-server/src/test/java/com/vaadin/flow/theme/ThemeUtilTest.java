package com.vaadin.flow.theme;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;

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

    @Test
    public void navigationTargetWithTheme_subclassGetsTheme() {
        ThemeDefinition theme = ThemeUtil.findThemeForNavigationTarget(
                ThemeSingleNavigationTargetSubclass.class, "single");
        Assert.assertNotNull(
                "Subclass should have a theme when the superclass has", theme);
        Assert.assertEquals(
                "Subclass should have the same theme as its superclass",
                MyTheme.class, theme.getTheme());
    }
}
