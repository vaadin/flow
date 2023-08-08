import { ComponentReference } from '../../component-util';
import { ComponentMetadata } from '../metadata/model';

export class ComponentOverlayManager {
  currentActiveComponent: ComponentReference | null = null;
  currentActiveComponentMetaData: ComponentMetadata | null = null;

  componentPicked = async (component: ComponentReference, metaData: ComponentMetadata) => {
    await this.hideOverlay();
    this.currentActiveComponent = component;
    this.currentActiveComponentMetaData = metaData;
  };

  showOverlay = () => {
    if (!this.currentActiveComponent || !this.currentActiveComponentMetaData) {
      return;
    }
    if (this.currentActiveComponentMetaData.openOverlay) {
      this.currentActiveComponentMetaData.openOverlay(this.currentActiveComponent);
    }
  };
  hideOverlay = () => {
    if (!this.currentActiveComponent || !this.currentActiveComponentMetaData) {
      return;
    }
    if (this.currentActiveComponentMetaData.hideOverlay) {
      this.currentActiveComponentMetaData.hideOverlay(this.currentActiveComponent);
    }
  };
  reset = () => {
    this.currentActiveComponent = null;
    this.currentActiveComponentMetaData = null;
  };
}
export const componentOverlayManager = new ComponentOverlayManager();

export const defaultShowOverlay = (component: ComponentReference) => {
  if (!component || !component.element) {
    return;
  }
  const element = component.element as any;
  //opening overlay
  element.opened = true;
  //preventing should remove focus to keep overlay opened.
  element._storedShouldRemoveFocus = element._shouldRemoveFocus;
  element._shouldRemoveFocus = (event: FocusEvent) => {
    event.preventDefault();
    event.stopImmediatePropagation();
    return false;
  };
  // preventing overlay to be closed while editing
  if (element._overlayElement) {
    element.overlayCloseOverrideEvent = element._overlayElement.addEventListener(
      'vaadin-overlay-close',
      (e: CustomEvent) => {
        e.preventDefault();
      }
    );
  }
};

export const defaultHideOverlay = (component: ComponentReference) => {
  if (component && component.element) {
    // restoring overridden listeners and methods.
    const element = component.element as any;
    if(element._storedShouldRemoveFocus){
      element._shouldRemoveFocus = element._storedShouldRemoveFocus;
    }
    delete element._storedShouldRemoveFocus;
    if (element._overlayElement) {
      element.removeEventListener('vaadin-overlay-close', element.overlayCloseOverrideEvent);
      delete element.overlayCloseOverrideEvent;
    }
    element.opened = false;
  }
};

export const defaultShowOverlayComboBoxMixin = (component: ComponentReference) => {
  if (!component || !component.element) {
    return;
  }
  const element = component.element as any;
  //opening overlay
  element.opened = true;

  const comboBox = element.$.comboBox;
  const overlay = comboBox.$.overlay;
  if(!comboBox || !overlay){
    return;
  }
  overlay._storedOutsideClickListener = (e: any) => {
    e.stopPropagation();
    e.preventDefault();
    return false;
  };
  overlay.addEventListener('vaadin-overlay-outside-click', overlay._storedOutsideClickListener);
  comboBox.removeEventListener('focusout', comboBox._boundOnFocusout);
}
export const defaultHideOverlayComboBoxMixin = (component: ComponentReference) => {
  if (!component || !component.element) {
    return;
  }
  const element = component.element as any;
  //closing the overlay
  element.opened = false;
  const comboBox = element.$.comboBox;
  const overlay = comboBox.$.overlay;
  if(!comboBox || !overlay){
    return;
  }
  overlay.removeEventListener('vaadin-overlay-outside-click', overlay._storedOutsideClickListener);
  delete overlay._storedOutsideClickListener;
  comboBox.addEventListener('focusout', comboBox._boundOnFocusout);
}