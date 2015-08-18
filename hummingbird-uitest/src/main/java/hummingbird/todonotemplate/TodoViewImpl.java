package hummingbird.todonotemplate;

import java.util.List;

import com.vaadin.annotations.Tag;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;

import hummingbird.todonotemplate.TodoFooter.Filter;

@Tag("section")
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
    private TodoList todoList;

    public TodoViewImpl() {
        setSizeUndefined();
        setId("todoapp");

        // Header
        CssLayout header = new CssLayout("section");
        HTML text = new HTML("<h1>todos</h1>");
        header.addComponent(text);
        header.setId("header");
        header.setSizeUndefined();

        NewTodo newTodo = new NewTodo();
        header.addComponent(newTodo);

        addComponent(header);

        // Main
        main = new CssLayout("section");
        main.setId("main");

        toggleAll = new CheckBox("Mark all as complete");
        toggleAll.setId("toggle-all");
        main.addComponent(toggleAll);
        todoList = new TodoList();
        todoList.setId("todo-list");
        main.addComponent(todoList);
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
        todoList.addComponent(row);
        row.setTodo(todo);
    }

    @Override
    public void removeTodo(Todo todo) {
        todoList.removeComponent(getRow(todo));
    }

    @Override
    public void refresh(Todo todo) {
        getRow(todo).setTodo(todo);
        applyFilter();
    }

    @Override
    public void refresh(List<Todo> todos) {
        todoList.removeAllComponents();
        for (Todo t : todos) {
            addTodo(t);
        }

        applyFilter();
    }

    private TodoRow getRow(Todo todo) {
        for (Component c : todoList) {
            TodoRow r = (TodoRow) c;
            if (r.getTodo() == todo) {
                return r;
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