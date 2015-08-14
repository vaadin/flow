package hummingbird.todonotemplate;

import java.util.List;

public interface TodoView {

    void refresh(Todo todo);

    void refresh(List<Todo> todos);

    void updateCounters(int completed, int active);

    void addTodo(Todo todo);

    void removeTodo(Todo todo);

}