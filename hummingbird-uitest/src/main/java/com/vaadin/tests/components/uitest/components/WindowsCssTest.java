package com.vaadin.tests.components.uitest.components;

import com.vaadin.tests.components.uitest.TestSampler;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class WindowsCssTest extends VerticalLayout {

    private TestSampler parent;
    private String styleName = null;
    private String caption = "A caption";

    private int debugIdCounter = 0;

    public WindowsCssTest(TestSampler parent) {
        this.parent = parent;
        parent.registerComponent(this);

        Button defWindow = new Button("Default window",
                new Button.ClickListener() {

                    @Override
                    public void buttonClick(ClickEvent event) {
                        createWindowWith(caption, null, styleName);
                    }
                });
        defWindow.setId("windButton" + debugIdCounter++);
        Button topToolbar = new Button("top toolbar window",
                new Button.ClickListener() {

                    @Override
                    public void buttonClick(ClickEvent event) {
                        createWindowWith(caption, ValoTheme.WINDOW_TOP_TOOLBAR,
                                styleName);
                    }
                });
        topToolbar.setId("windButton" + debugIdCounter++);
        Button bottomToolbar = new Button("bottom toolbar window",
                new Button.ClickListener() {

                    @Override
                    public void buttonClick(ClickEvent event) {
                        createWindowWith(caption,
                                ValoTheme.WINDOW_BOTTOM_TOOLBAR, styleName);
                    }
                });
        bottomToolbar.setId("windButton" + debugIdCounter++);

        addComponent(defWindow);
        addComponent(topToolbar);
        addComponent(bottomToolbar);

    }

    /**
     *
     * @param caption
     * @param primaryStyleName
     *            - the style defined styleName
     * @param styleName
     *            - the user defined styleName
     * @return
     */
    private void createWindowWith(String caption, String primaryStyleName,
            String styleName) {

        Window window = new Window();
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        window.setContent(layout);
        layout.addComponent(new Label("Some content"));

        if (caption != null) {
            window.setCaption(caption);
        }

        if (primaryStyleName != null) {
            window.addStyleName(primaryStyleName);
        }

        if (styleName != null) {
            window.addStyleName(styleName);
        }

        parent.getUI().addWindow(window);

    }

    @Override
    public void addStyleName(String style) {
        styleName = style;
    }

    @Override
    public void removeStyleName(String style) {
        styleName = null;
    }
}
