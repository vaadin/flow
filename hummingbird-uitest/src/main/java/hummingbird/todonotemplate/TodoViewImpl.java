package hummingbird.todonotemplate;

import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import hummingbird.todonotemplate.TodoFooter.Filter;

public class TodoViewImpl extends CssLayout implements TodoView {
    private NewTodo newTodo;
    private CssLayout main;
    private CheckBox toggleAll;

    private TodoPresenter presenter = new TodoPresenter(this);
    private TodoFooter footer;
    private Filter filter = Filter.ALL;

    /**
     * This sucks. Needed to distinguish between user value change and
     * programmatic value change: https://dev.vaadin.com/ticket/17820
     */
    private boolean toggleAllEvents = true;

    public TodoViewImpl() {
        setSizeUndefined();
        setId("todoapp");

        Label header = new Label("<h1>todos</h1>", ContentMode.HTML);
        header.setId("header");
        header.setSizeUndefined();
        addComponent(header);

        newTodo = new NewTodo();
        addComponent(newTodo);

        main = new CssLayout();
        main.setId("main");
        toggleAll = new CheckBox("Mark all as complete");
        toggleAll.setId("toggle-all");
        main.addComponent(toggleAll);
        addComponent(main);

        toggleAll.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                if (toggleAllEvents) {
                    getPresenter().markAllCompleted(toggleAll.getValue());
                }
            }
        });

        footer = new TodoFooter();
        addComponent(footer);
    }

    @Override
    public void updateCounters(int completed, int active) {
        footer.updateCounters(completed, active);
        toggleAllEvents = false;
        toggleAll.setValue(active == 0);
        toggleAllEvents = true;
    }

    public TodoPresenter getPresenter() {
        return presenter;
    }

    @Override
    public void addTodo(Todo todo) {
        TodoRow row = new TodoRow();
        main.addComponent(row);
        row.setTodo(todo);
    }

    @Override
    public void removeTodo(Todo todo) {
        main.removeComponent(getRow(todo));
    }

    @Override
    public void refresh(Todo todo) {
        getRow(todo).setTodo(todo);
        applyFilter();
    }

    @Override
    public void refresh(List<Todo> todos) {
        main.removeAllComponents();
        main.addComponent(toggleAll);
        for (Todo t : todos) {
            addTodo(t);
        }

        applyFilter();
    }

    private TodoRow getRow(Todo todo) {
        for (Component c : main) {
            if (c instanceof TodoRow) {
                TodoRow r = (TodoRow) c;
                if (r.getTodo() == todo) {
                    return r;
                }
            }
        }
        return null;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
        applyFilter();
    }

    private void applyFilter() {
        for (Component c : main) {
            if (c instanceof TodoRow) {
                TodoRow row = (TodoRow) c;
                row.setVisible(filter.pass(row.getTodo().isCompleted()));
            }
        }
    }
}