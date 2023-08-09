import {ComponentReference, getComponent} from '../../component-util';
import {ComponentMetadata} from '../metadata/model';

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
  const element = component.element as any;
  const comboBox = element.$.comboBox;
  const overlay = comboBox.$.overlay;
  showOverlayMixin(component.element, comboBox, overlay);
};

export const defaultHideOverlay = (component: ComponentReference) => {
  const element = component.element as any;
  const comboBox = element.$.comboBox;
  const overlay = comboBox.$.overlay;
  hideOverlayMixin(element, comboBox, overlay);
};

export const showOverlayMixin = (element: any, overlayMixinElement: any, overlay: any) => {
  //opening overlay
  element.opened = true;
  overlay._storedModeless = overlay.modeless;
  overlay.modeless = true;
  (document as any)._themeEditorDocClickListener = (e: MouseEvent) => {
    const target = e.target;
    if (target == null) {
      return;
    }
    if(shouldHideOverlayWhenClicked(target as HTMLElement, element as HTMLElement)){
      overlayMixinElement.opened = false;
    }
  };
  document.addEventListener('click', (document as any)._themeEditorDocClickListener);
  overlayMixinElement.removeEventListener('focusout', overlayMixinElement._boundOnFocusout);
}
export const hideOverlayMixin = (element: any, overlayMixinElement: any, overlay: any) => {
  //closing the overlay
  element.opened = false;
  if(!overlayMixinElement || !overlay){
    return;
  }
  overlay.modeless = overlay._storedModeless;
  delete overlay._storedModeless;
  overlayMixinElement.addEventListener('focusout', overlayMixinElement._boundOnFocusout);
  document.removeEventListener('click', (document as any)._themeEditorDocClickListener);
}

/**
 * Determines to hide overlay based on click position hits on Vaadin Dev tools.
 * @param clickedElement
 * @param pickedElement
 */
function shouldHideOverlayWhenClicked(clickedElement: HTMLElement, pickedElement: HTMLElement) {
  if(!clickedElement || !clickedElement.tagName){
    return true;
  }
  if(clickedElement.tagName.startsWith("VAADIN-DEV")){
    return false;
  }
  let element = clickedElement;
  let clickedComponentRef : ComponentReference = { nodeId: -1, uiId: -1, element: undefined };
  while(element && element.parentNode){
    clickedComponentRef = getComponent(element);
    if(clickedComponentRef.nodeId === -1){
      element = element.parentElement ? element.parentElement : ((element.parentNode as ShadowRoot).host as HTMLElement);
    }else{
      break;
    }
  }
  const pickedElementRef = getComponent(pickedElement);

  return !(clickedComponentRef.nodeId !== -1 && pickedElementRef.nodeId === clickedComponentRef.nodeId);

}
