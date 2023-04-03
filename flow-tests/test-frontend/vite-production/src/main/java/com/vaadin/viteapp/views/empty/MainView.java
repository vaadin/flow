package com.vaadin.viteapp.views.empty;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route("")
@JsModule("@testscope/button")
@JsModule("@testscope/map")
@JsModule("package-outside-npm/index.js")
@JsModule("package2-outside-npm/index.js")
@CssImport("./image.css")
public class MainView extends Div {

    public static final String PLANT = "plant";
    public static final String HIDEPLANT = "hideplant";
    public static final String OUTSIDE = "outsideButton";
    public static final String OUTSIDE_RESULT = "outsideResult";

    public MainView() {
        Image img = new Image("themes/vite-production/images/plant.png",
                "placeholder plant");
        img.setId(PLANT);
        img.setWidth("200px");
        add(img);

        add(new H2("This place intentionally left empty"));
        add(new Paragraph("It’s a place where you can grow your own UI 🤗"));

        NativeButton button = new NativeButton("Show/hide plant", e -> {
            img.setVisible(!img.isVisible());
        });
        button.setId(HIDEPLANT);
        add(button);
        setSizeFull();
        getStyle().set("text-align", "center");

        NativeButton checkOutsideJs = new NativeButton("Check outside JS",
                e -> {
                    getElement().executeJs(OUTSIDE_RESULT
                            + ".innerText = window.packageOutsideNpm() + ' - ' + window.package2OutsideNpm();");
                });
        checkOutsideJs.setId(OUTSIDE);
        add(checkOutsideJs);
        Paragraph outsideStatus = new Paragraph();
        outsideStatus.setId(OUTSIDE_RESULT);
        add(outsideStatus);

        add(new HtmlComponent("testscope-button"));
        add(new HtmlComponent("testscope-map"));
    }

}
