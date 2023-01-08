import { activateShim, ComponentReference, deactivateShim, getComponent, getComponents, highlight } from './shim';
import './vaadin-dnd.js';

type AddHandler = (
  referenceComponent: ComponentReference,
  where: Where,
  conponentType: 'Button' | 'TextField',
  constructorArguments: string[]
) => void;
type MoveHandler = (componentHierarchy: ComponentReference[], componentHierarchySelectedIndex: number) => void;

export type Where = 'inside' | 'before' | 'after';

let activeAddHandler: AddHandler | undefined;
let activeMoveHandler: MoveHandler | undefined;
let addComponent: ComponentReference | undefined;
let restoreElement: (() => void) | undefined;

let componentHierarchy: ComponentReference[] = [];
let componentHierarchySelected = -1;

let outlet: HTMLElement | null;
let dnd: HTMLElement | null;

export function activateAddMode(addHandler: AddHandler, moveHandler: MoveHandler) {
  activeAddHandler = addHandler;
  activeMoveHandler = moveHandler;
  dnd = document.createElement('vaadin-dev-tools-dnd');
  dnd.addEventListener('vaadin-dnd-drop', (e) => {
    const detail = (e as any).detail;
    const src = detail.element;
    if (src.hasAttribute('palette')) {
      let componentType: 'Button'|'TextField';
      const text = src.querySelector('template').content.firstElementChild.innerText;
      if (src.innerText.includes('Button')) {
        componentType = 'Button';
      } else {
        componentType = 'TextField';
      }
      let referenceElement;
      let where: Where;
      if (detail.insertBefore) {
        referenceElement = detail.insertBefore;
        where = 'before';
      } else {
        // add to empty layout
        referenceElement = detail.parent;
        where = 'inside';
      }
      addHandler(getComponent(referenceElement), where, componentType, [text]);
    } else {
      debugger;
    }
    console.log(e);
  });
  outlet = document.querySelector('#outlet');
  wrap(outlet!, dnd);
  dnd.append(document.createElement('dnd-palette'));
  // activateShim(shimMove, shimClick);
}
export function deactiveAddMode() {
  // deactivateShim();
  activeAddHandler = undefined;

  outlet!.parentElement!.parentElement!.append(outlet!);
  dnd!.remove();

  dnd = null;
  outlet = null;
}

function shimMove(targetElement: HTMLElement, _e: MouseEvent): void {
  componentHierarchy = getComponents(targetElement);
  componentHierarchySelected = componentHierarchy.length - 1;
  updateComponentHierarchy(componentHierarchy, componentHierarchySelected);
}

function shimClick(_targetElement: HTMLElement, _e: MouseEvent): void {
  deactivateShim();
}

function commitAdd(where: Where) {
  // activeAddHandler!(addComponent!, where);
  stopAdding();
  // newValueAssigner!();
}
function abortAdd() {
  stopAdding();
}

function stopAdding() {
  addComponent = undefined;
  if (restoreElement) {
    restoreElement();
    restoreElement = undefined;
  }

  activateShim(shimMove, shimClick);
}
export function handleKeyEvent(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    stopAdding();
    e.stopPropagation();
    e.preventDefault();
  } else if (e.key === 'ArrowUp') {
    let selected = componentHierarchySelected - 1;
    if (selected < 0) {
      selected = componentHierarchy.length - 1;
    }
    updateComponentHierarchy(componentHierarchy, selected);
  } else if (e.key === 'ArrowDown') {
    const selected = (componentHierarchySelected + 1) % componentHierarchy.length;
    updateComponentHierarchy(componentHierarchy, selected);
  }
}
function updateComponentHierarchy(hierarchy: ComponentReference[], selected: number) {
  componentHierarchy = hierarchy;
  componentHierarchySelected = selected;

  // highlight(componentHierarchy[componentHierarchySelected].element);
  let component = componentHierarchy[componentHierarchySelected].element!;
  // if (componentHierarchySelected > 0 && !isLayout(layout)) {
  //   layout = componentHierarchy[componentHierarchySelected-1].element!;
  // }
  // addSlots(layout);
  highlight(component);
  activeMoveHandler!(componentHierarchy, componentHierarchySelected);
}

function wrap(element: HTMLElement, wrapWith: HTMLElement) {
  element.parentElement?.insertBefore(wrapWith, element);
  wrapWith.append(element);
}
