package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@PreserveOnRefresh
@Route(value = "com.vaadin.flow.uitest.ui.ComplexDialogShortcutView")
public class ComplexDialogShortcutView extends DialogShortcutView {

    public static final String OVERLAY_ID = "overlay";
    private Div fakeOverlayElement;

    public ComplexDialogShortcutView() {
    }

    @Override
    public void open(Dialog dialog) {
        super.open(dialog);
        // the shortcut listener is not added yet, add the element locator data
        final String overlayId = OVERLAY_ID + dialog.index;
        final String overlayFetchJS = "document.getElementById('" + overlayId
                + "')";
        Shortcuts.setShortcutListenOnElement(overlayFetchJS, dialog);
        // simulate a fake overlay element
        fakeOverlayElement = new Div();
        fakeOverlayElement.setId(overlayId);
        getUI().orElse(UI.getCurrent()).add(fakeOverlayElement);
        // transport the dialog contents to overlay element
        dialog.getElement().executeJs(
                overlayFetchJS + ".appendChild(this.firstElementChild);");
    }

    @Override
    public void close(Dialog dialog) {
        super.close(dialog);

        fakeOverlayElement.getUI()
                .ifPresent(ui -> ui.remove(fakeOverlayElement));
    }
}
