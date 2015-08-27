package com.vaadin.ui;

public interface Layer extends Component {
    public int UI_LAYER_INDEX = 1;
    public int WINDOW_LAYER_INDEX = 10000;

    public int getLayerIndex();

    public void addComponent(Component c);

    public void removeComponent(Component c);
}
