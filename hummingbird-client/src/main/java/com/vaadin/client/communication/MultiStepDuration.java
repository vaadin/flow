package com.vaadin.client.communication;

import com.google.gwt.core.client.Duration;
import com.vaadin.client.Console;

public class MultiStepDuration extends Duration {
    private int previousStep = elapsedMillis();

    public void logDuration(String message) {
        logDuration(message, 0);
    }

    public void logDuration(String message, int minDuration) {
        int currentTime = elapsedMillis();
        int stepDuration = currentTime - previousStep;
        if (stepDuration >= minDuration) {
            Console.log(message + ": " + stepDuration + " ms");
        }
        previousStep = currentTime;
    }
}
