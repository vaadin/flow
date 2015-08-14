package hummingbird.todonotemplate;

import java.util.ArrayList;
import java.util.List;

public class TodoPresenter {

    private TodoView view;

    // "Model"
    private TodoModel model = new TodoModel();

    public static class TodoModel {
        private List<Todo> todos = new ArrayList<>();

        public int getCompleted() {
            int nr = 0;
            for (Todo t : todos) {
                if (t.isCompleted()) {
                    nr++;
                }
            }
            return nr;
        }

        public int getActive() {
            return todos.size() - getCompleted();
        }
    }

    public TodoPresenter(TodoView view) {
        this.view = view;
    }

    public void markCompleted(Todo todo, boolean completed) {
        todo.setCompleted(completed);
        view.refresh(todo);
        view.updateCounters(model.getCompleted(), model.getActive());
    }

    public void updateText(Todo todo, String value) {
        todo.setText(value);
        model.todos.set(model.todos.indexOf(todo), todo);
        view.refresh(todo);
    }

    public void add(Todo todo) {
        model.todos.add(todo);
        view.addTodo(todo);
        view.updateCounters(model.getCompleted(), model.getActive());
    }

    public void delete(Todo todo) {
        model.todos.remove(todo);

        view.removeTodo(todo);
        view.updateCounters(model.getCompleted(), model.getActive());
    }

    public void clearCompleted() {
        for (Todo t : model.todos.toArray(new Todo[model.todos.size()])) {
            if (t.isCompleted()) {
                model.todos.remove(t);
            }
        }

        view.refresh(model.todos);
    }

    public void markAllCompleted(boolean completed) {
        for (Todo t : model.todos.toArray(new Todo[model.todos.size()])) {
            t.setCompleted(completed);
        }
        view.refresh(model.todos);
    }

}