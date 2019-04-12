package com.vaadin.flow.server.frontend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.vaadin.flow.server.frontend.ClassPathIntrospector.DefaultClassFinder;
import com.vaadin.flow.server.frontend.TestComponents.FirstView;
import com.vaadin.flow.server.frontend.TestComponents.RootViewWithLayoutTheme;
import com.vaadin.flow.server.frontend.TestComponents.RootViewWithMultipleTheme;
import com.vaadin.flow.server.frontend.TestComponents.RootViewWithTheme;
import com.vaadin.flow.server.frontend.TestComponents.RootViewWithoutTheme;
import com.vaadin.flow.server.frontend.TestComponents.SecondView;
import com.vaadin.flow.server.frontend.TestComponents.Theme1;
import com.vaadin.flow.server.frontend.TestComponents.Theme2;
import com.vaadin.flow.server.frontend.TestComponents.Theme4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FrontendDependenciesTest {

    @Test
    public void should_CorrectlyExtractClassesFromSignatures() {
        Set<String> classes = new HashSet<>();
        FrontendDependencies.addSignatureToClasses(classes,
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

        FrontendDependencies.addSignatureToClasses(classes,
                "([Lcom/vaadin/flow/component/Component;)V");
        assertEquals(12, classes.size());
        assertTrue(classes.contains("com.vaadin.flow.component.Component"));

        FrontendDependencies.addSignatureToClasses(classes,
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

        assertEquals(Theme4.class, deps.getTheme());

        assertEquals(0, deps.getAllImports().size());

        assertEquals(1, deps.getAllModules().size());
        assertTrue(deps.getAllModules().contains("./theme-4.js"));

        assertEquals(1, deps.getAllPackages().size());
        assertTrue(deps.getAllPackages().contains("@vaadin/theme-0"));

        assertEquals(1, deps.getAllScripts().size());
        assertTrue(deps.getAllScripts().contains("frontend://theme-0.js"));
    }

    @Test
    public void should_not_takeTheme_when_NoTheme() {
        FrontendDependencies deps = create(RootViewWithoutTheme.class);

        assertEquals(null, deps.getTheme());

        assertEquals(0, deps.getAllImports().size());
        assertEquals(2, deps.getAllModules().size());
        assertEquals(0, deps.getAllPackages().size());
        assertEquals(1, deps.getAllScripts().size());
    }

    @Test
    public void should_takeThemeFromLayout() {
        FrontendDependencies deps = create(RootViewWithLayoutTheme.class);

        assertEquals(Theme1.class, deps.getTheme());

        assertEquals(2, deps.getAllImports().size());
        assertEquals(8, deps.getAllModules().size());
        assertEquals(3, deps.getAllPackages().size());
        assertEquals(6, deps.getAllScripts().size());
    }


    @Test
    public void should_takeThemeFromView_when_MultipleTheme() {
        FrontendDependencies deps = create(RootViewWithMultipleTheme.class);

        assertEquals(Theme2.class, deps.getTheme());

        assertEquals(2, deps.getAllImports().size());
        assertEquals(4, deps.getAllModules().size());
        assertEquals(2, deps.getAllPackages().size());
        assertEquals(2, deps.getAllScripts().size());
    }

    @Test
    public void should_not_takeTheme_when_NoRootView() {
        FrontendDependencies deps = create(SecondView.class);

        assertEquals(null, deps.getTheme());

        assertEquals(1, deps.getAllImports().size());
        assertEquals(4, deps.getAllModules().size());
        assertEquals(2, deps.getAllPackages().size());
        assertEquals(2, deps.getAllScripts().size());
    }

    @Test
    public void should_summarize_when_MultipleViews() {
        FrontendDependencies deps = create(SecondView.class, FirstView.class);

        assertEquals(Theme1.class, deps.getTheme());

        assertEquals(2, deps.getAllImports().size());
        assertEquals(8, deps.getAllModules().size());
        assertEquals(3, deps.getAllPackages().size());
        assertEquals(6, deps.getAllScripts().size());
    }
}
