package com.vaadin.viteapp.views.empty;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.dependencies.ThemableTextField;

@Route("")
@JsModule("./jsonloader.js")
@JsModule("package-outside-npm/index.js")
@JsModule("package2-outside-npm/index.js")
@JsModule("./testscopebuttonloader.js")
@CssImport(value = "./cssimport-textfield.css", themeFor = "vaadin-text-field")
@CssImport(value = "./cssimport.css")
public class MainView extends Div {

    public static final String LOAD_AND_SHOW_JSON = "loadAndShowJson";
    public static final String JSON_CONTAINER = "jsonContainer";
    public static final String PLANT = "plant";
    public static final String OUTSIDE = "outsideButton";
    public static final String OUTSIDE_RESULT = "outsideResult";

    public MainView() {
        Image img = new Image("themes/vite-basics/images/plant.png",
                "placeholder plant");
        img.setId(PLANT);
        img.setWidth("200px");
        add(img);

        add(new H2("This place intentionally left empty"));
        add(new Paragraph("It’s a place where you can grow your own UI 🤗"));

        NativeButton button = new NativeButton("Show/hide plant", e -> {
            img.setVisible(!img.isVisible());
        });
        add(button);

        Div jsonContainer = new Div();
        jsonContainer.setId(JSON_CONTAINER);
        NativeButton loadAndShowJson = new NativeButton("Load and show JSON",
                e -> {
                    getElement().executeJs(
                            "const json = window.loadJson(json => $0.innerText=json);",
                            jsonContainer);
                });
        loadAndShowJson.setId(LOAD_AND_SHOW_JSON);
        add(button, loadAndShowJson, jsonContainer);
        setSizeFull();
        getStyle().set("text-align", "center");

        final Paragraph viteStatus = new Paragraph(
                "Vite feature is "
                        + !FeatureFlags
                                .get(UI.getCurrent().getSession().getService()
                                        .getContext())
                                .isEnabled(FeatureFlags.WEBPACK));
        viteStatus.setId("status");
        add(viteStatus);

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
        ThemableTextField textField = new ThemableTextField();
        textField.setId("themedfield");
        add(textField);
    }

}
