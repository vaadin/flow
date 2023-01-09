import { activateShim, ComponentReference, deactivateShim, getComponents, highlight } from './shim';

let pickCallback: ((ref: ComponentReference) => void) | undefined;
let pickingCallback: ((components: ComponentReference[], index: number) => void) | undefined;
let pickCompleteCallback: (() => void) | undefined;

let selectedComponentIndex = 0;
let currentComponents: ComponentReference[] = [];

function updateComponents(components: ComponentReference[], selected: number) {
  selectedComponentIndex = selected;
  currentComponents = components;
  highlight(currentComponents[selectedComponentIndex].element!);
  pickingCallback!(currentComponents, selectedComponentIndex);
}

function shimMove(targetElement: HTMLElement, _e: MouseEvent) {
  const components = getComponents(targetElement);
  const selected = components.length - 1;
  updateComponents(components, selected);
}

function shimClick(targetElement: HTMLElement, _e: MouseEvent) {
  if (pickCallback) {
    const component = getComponents(targetElement)[selectedComponentIndex];
    doPickComponent(component);
  }
}

export function stopPickComponent() {
  deactivateShim();
  pickCompleteCallback!();
  pickCallback = undefined;
  pickingCallback = undefined;
  pickCompleteCallback = undefined;
}

function doPickComponent(component: ComponentReference) {
  pickCallback!({ nodeId: component.nodeId, appId: component.appId });
  stopPickComponent();
}

export function handlePickKeyEvent(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    stopPickComponent();
    e.stopPropagation();
    e.preventDefault();
  } else if (e.key === 'ArrowUp') {
    let selected = selectedComponentIndex - 1;
    if (selected < 0) {
      selected = currentComponents.length - 1;
    }
    updateComponents(currentComponents, selected);
  } else if (e.key === 'ArrowDown') {
    const selected = (selectedComponentIndex + 1) % currentComponents.length;
    updateComponents(currentComponents, selected);
  } else if (e.key === 'Enter') {
    doPickComponent(currentComponents[selectedComponentIndex]);
    e.stopPropagation();
    e.preventDefault();
  }
}
export function pickComponent(
  onPick: (ref: ComponentReference) => void,
  onPicking: (components: ComponentReference[], index: number) => void,
  onComplete: () => void
) {
  pickCallback = onPick;
  pickingCallback = onPicking;
  pickCompleteCallback = onComplete;
  activateShim(shimMove, shimClick, () => {});
}
