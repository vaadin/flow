package com.vaadin.tests.server;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.annotations.Tag;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.hummingbird.kernel.DomEventListener;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.Component;

import elemental.json.JsonObject;

@Tag("button")
public class TestButton extends TestComponent {

    public TestButton() {

    }

    public TestButton(String text) {
        setText(text);
    }

    public void setText(String text) {
        getElement().setTextContent(text);
    }

    public String getText() {
        return getElement().getTextContent();
    }

    /**
     * Click event. This event is thrown, when the button is clicked.
     *
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public static class TestClickEvent extends Component.Event {

        private final MouseEventDetails details;

        /**
         * New instance of text change event.
         *
         * @param source
         *            the Source of the event.
         */
        public TestClickEvent(Component source) {
            super(source);
            details = null;
        }

        /**
         * Constructor with mouse details
         *
         * @param source
         *            The source where the click took place
         * @param details
         *            Details about the mouse click
         */
        public TestClickEvent(Component source, MouseEventDetails details) {
            super(source);
            this.details = details;
        }

        /**
         * Returns the mouse position (x coordinate) when the click took place.
         * The position is relative to the browser client area.
         *
         * @return The mouse cursor x position or -1 if unknown
         */
        public int getClientX() {
            if (null != details) {
                return details.getClientX();
            } else {
                return -1;
            }
        }

        /**
         * Returns the mouse position (y coordinate) when the click took place.
         * The position is relative to the browser client area.
         *
         * @return The mouse cursor y position or -1 if unknown
         */
        public int getClientY() {
            if (null != details) {
                return details.getClientY();
            } else {
                return -1;
            }
        }

        /**
         * Returns the relative mouse position (x coordinate) when the click
         * took place. The position is relative to the clicked component.
         *
         * @return The mouse cursor x position relative to the clicked layout
         *         component or -1 if no x coordinate available
         */
        public int getRelativeX() {
            if (null != details) {
                return details.getRelativeX();
            } else {
                return -1;
            }
        }

        /**
         * Returns the relative mouse position (y coordinate) when the click
         * took place. The position is relative to the clicked component.
         *
         * @return The mouse cursor y position relative to the clicked layout
         *         component or -1 if no y coordinate available
         */
        public int getRelativeY() {
            if (null != details) {
                return details.getRelativeY();
            } else {
                return -1;
            }
        }

        /**
         * Checks if the Alt key was down when the mouse event took place.
         *
         * @return true if Alt was down when the event occured, false otherwise
         *         or if unknown
         */
        public boolean isAltKey() {
            if (null != details) {
                return details.isAltKey();
            } else {
                return false;
            }
        }

        /**
         * Checks if the Ctrl key was down when the mouse event took place.
         *
         * @return true if Ctrl was pressed when the event occured, false
         *         otherwise or if unknown
         */
        public boolean isCtrlKey() {
            if (null != details) {
                return details.isCtrlKey();
            } else {
                return false;
            }
        }

        /**
         * Checks if the Meta key was down when the mouse event took place.
         *
         * @return true if Meta was pressed when the event occured, false
         *         otherwise or if unknown
         */
        public boolean isMetaKey() {
            if (null != details) {
                return details.isMetaKey();
            } else {
                return false;
            }
        }

        /**
         * Checks if the Shift key was down when the mouse event took place.
         *
         * @return true if Shift was pressed when the event occured, false
         *         otherwise or if unknown
         */
        public boolean isShiftKey() {
            if (null != details) {
                return details.isShiftKey();
            } else {
                return false;
            }
        }
    }

    /**
     * Interface for listening for a {@link ClickEvent} fired by a
     * {@link Component}.
     *
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public interface TestClickListener extends EventListener, Serializable {

        /**
         * Called when a {@link Button} has been clicked. A reference to the
         * button is given by {@link ClickEvent#getButton()}.
         *
         * @param event
         *            An event containing information about the click.
         */
        public void buttonClick(TestClickEvent event);
    }

    /**
     * Adds the button click listener.
     *
     * @param listener
     *            the Listener to be added.
     */
    public void addClickListener(TestClickListener listener) {
        if (!hasListeners(TestClickEvent.class)) {
            getElement().addEventData("click",
                    MouseEventDetails.getEventProperties());
            getElement().addEventListener("click", new DomEventListener() {
                @Override
                public void handleEvent(JsonObject eventData) {
                    fireClick(new MouseEventDetails(eventData));
                }
            });
        }
        addListener(TestClickEvent.class, listener);
    }

    /**
     * Removes the button click listener.
     *
     * @param listener
     *            the Listener to be removed.
     */
    public void removeClickListener(TestClickListener listener) {
        removeListener(TestClickEvent.class, listener);
    }

    /**
     * Fires a click event to all listeners without any event details.
     *
     * In subclasses, override {@link #fireClick(MouseEventDetails)} instead of
     * this method.
     */
    protected void fireClick() {
        fireEvent(new TestClickEvent(this));
    }

    /**
     * Fires a click event to all listeners.
     *
     * @param details
     *            MouseEventDetails from which keyboard modifiers and other
     *            information about the mouse click can be obtained. If the
     *            button was clicked by a keyboard event, some of the fields may
     *            be empty/undefined.
     */
    protected void fireClick(MouseEventDetails details) {
        fireEvent(new TestClickEvent(this, details));
    }

}
