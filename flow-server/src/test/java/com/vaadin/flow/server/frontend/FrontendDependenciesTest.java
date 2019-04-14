package com.vaadin.flow.server.frontend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.vaadin.flow.server.frontend.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.FrontendTestComponents.FirstView;
import com.vaadin.flow.server.frontend.FrontendTestComponents.RootViewWithLayoutTheme;
import com.vaadin.flow.server.frontend.FrontendTestComponents.RootViewWithMultipleTheme;
import com.vaadin.flow.server.frontend.FrontendTestComponents.RootViewWithTheme;
import com.vaadin.flow.server.frontend.FrontendTestComponents.RootViewWithoutTheme;
import com.vaadin.flow.server.frontend.FrontendTestComponents.SecondView;
import com.vaadin.flow.server.frontend.FrontendTestComponents.Theme1;
import com.vaadin.flow.server.frontend.FrontendTestComponents.Theme2;
import com.vaadin.flow.server.frontend.FrontendTestComponents.Theme4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FrontendDependenciesTest {

    @Test
    public void should_extractClassesFromSignatures() {
        Set<String> classes = new HashSet<>();
        FrontendClassVisitor visitor = new FrontendClassVisitor(null, null);

        visitor.addSignatureToClasses(classes,
                "(Lcom/vaadin/flow/component/tabs/Tabs;Ljava/lang/String;Ljava/lang/Character;CLjava/lang/Integer;ILjava/lang/Long;JLjava/lang/Double;DLjava/lang/Float;FLjava/lang/Byte;BLjava/lang/Boolean;Z)Lcom/vaadin/flow/component/button/Button;");
        assertEquals(11, classes.size());
        assertArrayEquals(new String [] {
                "",
                "java.lang.Float",
                "com.vaadin.flow.component.button.Button",
                "java.lang.Character",
                "java.lang.Long",
                "java.lang.Double",
                "java.lang.Boolean",
                "com.vaadin.flow.component.tabs.Tabs",
                "java.lang.String",
                "java.lang.Byte",
                "java.lang.Integer"}, classes.toArray());

        visitor.addSignatureToClasses(classes,
                "([Lcom/vaadin/flow/component/Component;)V");
        assertEquals(12, classes.size());
        assertTrue(classes.contains("com.vaadin.flow.component.Component"));

        visitor.addSignatureToClasses(classes,
                "(Lcom/vaadin/flow/component/orderedlayout/FlexComponent$Alignment;[Lcom/vaadin/flow/component/Component;)");
        assertEquals(13, classes.size());
        assertTrue(classes.contains("com.vaadin.flow.component.orderedlayout.FlexComponent$Alignment"));
    }

    private FrontendDependencies create(Class<?> ...classes) {
        return new FrontendDependencies(new DefaultClassFinder(new HashSet<Class<?>>(Arrays.asList(classes))));
    }

    @Test
    public void should_takeThemeFromView() {
        FrontendDependencies deps = create(RootViewWithTheme.class);

        assertEquals(Theme4.class, deps.getThemeDefinition().getTheme());

        assertEquals(0, deps.getImports().size());

        assertEquals(1, deps.getModules().size());
        assertTrue(deps.getModules().contains("./theme-4.js"));

        assertEquals(1, deps.getPackages().size());
        assertTrue(deps.getPackages().contains("@vaadin/theme-0"));

        assertEquals(1, deps.getScripts().size());
        assertTrue(deps.getScripts().contains("frontend://theme-0.js"));
    }

    @Test
    public void should_not_takeTheme_when_NoTheme() {
        FrontendDependencies deps = create(RootViewWithoutTheme.class);

        assertNull(deps.getThemeDefinition());

        assertEquals(0, deps.getImports().size());
        assertEquals(2, deps.getModules().size());
        assertEquals(0, deps.getPackages().size());
        assertEquals(1, deps.getScripts().size());
    }

    @Test
    public void should_takeThemeFromLayout() {
        FrontendDependencies deps = create(RootViewWithLayoutTheme.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        assertEquals(2, deps.getImports().size());
        assertEquals(8, deps.getModules().size());
        assertEquals(3, deps.getPackages().size());
        assertEquals(6, deps.getScripts().size());
    }


    @Test
    public void should_takeThemeFromView_when_MultipleTheme() {
        FrontendDependencies deps = create(RootViewWithMultipleTheme.class);

        assertEquals(Theme2.class, deps.getThemeDefinition().getTheme());
        assertEquals("foo", deps.getThemeDefinition().getVariant());

        assertEquals(2, deps.getImports().size());
        assertEquals(4, deps.getModules().size());
        assertEquals(2, deps.getPackages().size());
        assertEquals(2, deps.getScripts().size());
    }

    @Test
    public void should_not_takeTheme_when_NoRootView() {
        FrontendDependencies deps = create(SecondView.class);

        assertNull(deps.getThemeDefinition());

        assertEquals(1, deps.getImports().size());
        assertEquals(4, deps.getModules().size());
        assertEquals(2, deps.getPackages().size());
        assertEquals(2, deps.getScripts().size());
    }

    @Test
    public void should_summarize_when_MultipleViews() {
        FrontendDependencies deps = create(SecondView.class, FirstView.class);

        assertEquals(Theme1.class, deps.getThemeDefinition().getTheme());

        assertEquals(2, deps.getImports().size());
        assertEquals(8, deps.getModules().size());
        assertEquals(3, deps.getPackages().size());
        assertEquals(6, deps.getScripts().size());
    }
}
