package com.vaadin.viteapp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;

public class LoginForm extends Div {
    private Input userName = new Input();
    private Input password = new Input();
    private Div errorMsg = new Div();
    private String userLabel;
    private String pwdLabel;
    private Div layout = new Div();
    private List<SerializableRunnable> loginListeners = new CopyOnWriteArrayList<>();

    public LoginForm() {
        updateForm();

        add(layout);

        NativeButton login = new NativeButton("Login", event -> login());
        add(login, errorMsg);
    }

    public void setUserNameLabel(String userNameLabelString) {
        userLabel = userNameLabelString;
        updateForm();
    }

    public void setPasswordLabel(String pwd) {
        pwdLabel = pwd;
        updateForm();
    }

    public void updateForm() {
        layout.removeAll();
        layout.add(new Span(userLabel), userName);
        layout.add(new Span(pwdLabel), password);
    }

    private void login() {
        Optional<Object> authToken = UserService.getInstance()
                .authenticate(userName.getValue(), password.getValue());
        if (authToken.isPresent()) {
            errorMsg.setText("Authentication success");
        } else {
            errorMsg.setText("Authentication failure");
        }
    }

}
