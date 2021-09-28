package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.theme.GlobalThemeView")
public class GlobalThemeView extends Div {

    public GlobalThemeView() {
        Span faText = new Span("This test is FontAwesome.");
        faText.setClassName("fas fa-coffee");
        faText.setId(ThemeView.FONTAWESOME_ID);

        add(faText);
    }
}
