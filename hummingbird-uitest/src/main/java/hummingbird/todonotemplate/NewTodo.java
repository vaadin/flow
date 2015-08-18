package hummingbird.todonotemplate;

import com.vaadin.ui.TextField;

public class NewTodo extends TextField {

    private boolean focused = false;

    public NewTodo() {
        setId("new-todo");
        setInputPrompt("What needs to be done?");

        addFocusListener((e) -> {
            focused = true;
        });
        addBlurListener((e) -> {
            focused = false;
        });
        addValueChangeListener(e -> {
            if (!getValue().isEmpty()) {
                getPresenter().add(new Todo(getValue()));
                clear();
            }
        });
        // addShortcutListener(new ShortcutListener(null, KeyCode.ENTER, null) {
        // @Override
        // public void handleAction(Object sender, Object target) {
        // if (!focused) {
        // // This is the strangest possible way to handle that enter
        // // in the edit field for an existing todo should submit the
        // // change
        // getUI().focus();
        // return;
        // }
        //
        // if (!getValue().isEmpty()) {
        // getPresenter().add(new Todo(getValue()));
        // clear();
        // }
        // }
        // });
    }

    protected TodoPresenter getPresenter() {
        return findAncestor(TodoViewImpl.class).getPresenter();
    }
}