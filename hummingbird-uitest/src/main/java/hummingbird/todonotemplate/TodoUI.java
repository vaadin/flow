package hummingbird.todonotemplate;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
@Theme("none")
@StyleSheet("TodoList.css")
public class TodoUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        setSizeUndefined();

        CssLayout root = new CssLayout();
        root.setId("root");
        setContent(root);

        TodoViewImpl todoViewImpl = new TodoViewImpl();
        root.addComponent(todoViewImpl);

        HTML info = new HTML("<div><p>Double-click to edit a todo</p>"
                + "<p>Written by <a href=\"https://github.com/jounik\">Jouni Koivuviita</a> and <a href=\"https://github.com/marlonrichert\">Marlon Richert</a></p>"
                + "<p>Part of <a href=\"http://todomvc.com\">TodoMVC</a></p></div>");
        info.setId("info");
        root.addComponent(info);
        for (int i = 0; i < 1000; i++) {
            todoViewImpl.getPresenter().add(new Todo("" + i));
        }
    }

}