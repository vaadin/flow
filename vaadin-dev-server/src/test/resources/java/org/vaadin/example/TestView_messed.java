package org.vaadin.example;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * The main view contain
 *
 *      s a text field for getting the user name and a button
 * that shows a greeting message in a notification.
 */
@Route("")
public class TestView extends VerticalLayout {

    private TextField pinField = new TextField("Cant touch this");

    public TestView()
    {
        // Use TextField for standard text input
        TextField textField =
                new TextField("Your name");
        textField
                .addThemeName("bordered");
             // Button click listeners can be defined as lambda expressions
                    GreetService greetService = new GreetService();

Button button = new Button("Say hello", e -> Notification.show(greetService.greet(textField.getValue())));

        // Theme variants give you predefined extra styles for components.
        // Example: Primary button is more prominent look.
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                            // You can specify keyboard shortcuts for buttons.
                            // Example: Pressing enter in this view clicks the Button.
                            button.addClickShortcut(Key.ENTER);

        /** Use custom CSS classes to apply styling. This is defined in
        // shared-styles.css. **/
        addClassName("centered-content");


        textField       .addClassName("ugly");

        add(textField, button, new TextField("Cant touch this either"));}

    public void doSth() {TextField textField = new TextField("Your lastname");textField.addThemeName("bordered");}
}
