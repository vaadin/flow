package hummingbird.todonotemplate;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.TextField;

public class TodoRow extends CssLayout {

    private CheckBox completed;
    private TextField edit;
    private Label caption;

    private Todo todo;

    public TodoRow() {
        addStyleName("todo-row");

        completed = new CheckBox(null);
        addComponent(completed);

        caption = new Label("");
        caption.setSizeUndefined();
        addComponent(caption);

        NativeButton destroy = new NativeButton();
        destroy.addStyleName("destroy");
        addComponent(destroy);

        edit = new TextField();
        addComponent(edit);

        /* Event handling */
        completed.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                getPresenter().markCompleted(todo, completed.getValue());
            }
        });

        addLayoutClickListener(new LayoutClickListener() {
            @Override
            public void layoutClick(LayoutClickEvent event) {
                if (event.isDoubleClick()
                        && caption == event.getClickedComponent()) {
                    setEditing(true);
                }
            }
        });

        // Blur instead of ValueChange as we want to go back to non-edit mode
        // also when there are no changes
        edit.addBlurListener(new BlurListener() {
            @Override
            public void blur(BlurEvent event) {
                getPresenter().updateText(todo, edit.getValue());
                setEditing(false);
            }
        });

        destroy.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                getPresenter().delete(todo);
            }
        });

    }

    private void setEditing(boolean editing) {
        getElement().setClass("editing", editing);
        if (editing) {
            edit.selectAll();
            edit.focus();
        }
    }

    public TodoPresenter getPresenter() {
        return findAncestor(TodoViewImpl.class).getPresenter();
    }

    public void setTodo(Todo todo) {
        this.todo = todo;
        getElement().setClass("completed", todo.isCompleted());
        completed.setValue(todo.isCompleted());
        caption.setValue(todo.getText());
        edit.setValue(todo.getText());
    }

    Todo getTodo() {
        return todo;
    }

}