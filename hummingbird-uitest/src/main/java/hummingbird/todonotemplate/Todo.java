package hummingbird.todonotemplate;

public class Todo {
    private String text = "";
    private boolean completed = false;

    public Todo() {

    }

    public Todo(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

}