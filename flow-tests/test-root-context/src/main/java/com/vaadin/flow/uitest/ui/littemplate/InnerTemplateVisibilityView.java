package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.InnerTemplateVisibilityView", layout = ViewTestLayout.class)
public class InnerTemplateVisibilityView extends AbstractDivView {

    public static final String TOGGLE_VISIBILITY_BUTTON_ID = "toggleVisibility";
    public static final String INNER_ID = "inner";
    public static final String OUTER_ID = "outer";

    @Tag("lit-template-inner")
    @JsModule("./lit/lit-template-inner.js")
    public static class Inner extends LitTemplate {
        public Inner() {
        }
    }

    @Tag("lit-template-outer")
    @JsModule("./lit/lit-template-outer.js")
    public static class Outer extends LitTemplate {
        @Id("inner")
        Inner inner;

        public Outer() {
        }
    }

    private boolean isVisible = true;

    public InnerTemplateVisibilityView() {
        Outer outer = new Outer();
        outer.setId(OUTER_ID);
        outer.inner.setId(INNER_ID);

        NativeButton toggleVisibilityButton = new NativeButton(
                "Toggle visibility of inner", e -> {
                    isVisible = !isVisible;
                    outer.inner.setVisible(isVisible);
                });
        toggleVisibilityButton.setId(TOGGLE_VISIBILITY_BUTTON_ID);

        add(toggleVisibilityButton, outer);
    }
}
