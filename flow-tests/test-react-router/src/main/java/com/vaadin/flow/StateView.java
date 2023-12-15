package com.vaadin.flow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.StateView")
public class StateView extends Div {
    protected static String ENABLED_SPAN = "enabled";
    protected static String REACT_SPAN = "react_added";

    public void StateView()  {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)  {
        Span enabled = new Span("React enabled: " +
                getUI().get().getSession().getConfiguration()
                        .isReactRouterEnabled());
        enabled.setId(ENABLED_SPAN);

        File baseDir = new File(System.getProperty("user.dir", "."));
        String packageJson = null;
        try {
            packageJson = FileUtils.readFileToString(
                    new File(baseDir, "package.json"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            packageJson = "";
        }

        Span react = new Span("React found: " + packageJson.contains("react"));
        react.setId(REACT_SPAN);

        add(enabled, new Div(), react);
    }

}