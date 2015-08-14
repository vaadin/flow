package hummingbird.todonotemplate;

import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
// @Theme("todomvc")
public class TodoUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        setSizeUndefined();

        CssLayout root = new CssLayout();
        setContent(root);

        root.addComponent(new TodoViewImpl());

        Label info = new Label(
                "<p>Double-click to edit a todo</p>"
                        + "<p>Written by <a href=\"https://github.com/jounik\">Jouni Koivuviita</a> and <a href=\"https://github.com/marlonrichert\">Marlon Richert</a></p>"
                        + "<p>Part of <a href=\"http://todomvc.com\">TodoMVC</a></p>",
                ContentMode.HTML);
        info.setId("info");
        root.addComponent(info);

    }

}