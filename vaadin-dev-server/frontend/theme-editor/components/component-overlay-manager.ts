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
    element._shouldRemoveFocus = element._storedShouldRemoveFocus;
    delete element._storedShouldRemoveFocus;
    if (element._overlayElement) {
      element.removeEventListener('vaadin-overlay-close', element.overlayCloseOverrideEvent);
      delete element.overlayCloseOverrideEvent;
    }
    element.opened = false;
  }
};
