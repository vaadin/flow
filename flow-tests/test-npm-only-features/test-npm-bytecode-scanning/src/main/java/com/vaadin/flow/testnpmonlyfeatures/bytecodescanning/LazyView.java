package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.LazyView", layout = ViewTestLayout.class)
public class LazyView extends Div {

    @Tag("lazy-component")
    @JsModule("./lazy-component.js")
    public static class LazyComponent extends Component {
    }

    public LazyView() {
        add(new Paragraph("Below this, you should see 'Lazy component'"));
        add(new LazyComponent());
    }
}
