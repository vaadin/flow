import { html, render } from 'lit';

let shim: HTMLElement | undefined;
let highlighted: HTMLElement | undefined;

type ShimEventHandler = (targetElement: HTMLElement, e: MouseEvent) => void;

let shimMoveHandler: ShimEventHandler | undefined;
let shimClickHandler: ((targetElement: HTMLElement, e: MouseEvent) => void) | undefined;

export function activateShim(moveHandler: ShimEventHandler, clickHandler: ShimEventHandler) {
  shimMoveHandler = moveHandler;
  shimClickHandler = clickHandler;
  if (!shim) {
    render(
      html`<style>
          #vaadin-dev-tools-shim {
            background: rgba(255, 255, 255, 0);
            position: absolute;
            inset: 0px;
            z-index: 1000000;
          }
        </style>
        <div id="vaadin-dev-tools-shim" @mousemove=${shimMove} @click=${shimClick}></div> `,
      document.body
    );
    shim = document.querySelector('#vaadin-dev-tools-shim') as HTMLElement;
  }
  shim.style.display = 'block';
}

function shimClick(e: MouseEvent) {
  if (!shimClickHandler) {
    return;
  }
  shimClickHandler(getTargetElement(e), e);
}
function shimMove(e: MouseEvent) {
  if (!shimMoveHandler) {
    return;
  }
  shimMoveHandler(getTargetElement(e), e);
}

export function deactivateShim() {
  if (shim) {
    shim.style.display = 'none';
  }
  highlight(undefined);
  shimMoveHandler = undefined;
  shimClickHandler = undefined;
}

function getTargetElement(e: MouseEvent): HTMLElement {
  shim!.style.display = 'none';
  const targetElement = document.elementFromPoint(e.clientX, e.clientY) as HTMLElement;
  shim!.style.display = '';
  return targetElement;
}

export function highlight(element: HTMLElement | undefined) {
  if (highlighted) {
    highlighted.style.outline = '';
  }
  highlighted = element;
  if (highlighted) {
    highlighted.style.outline = '1px solid red';
  }
}
export function getComponents(element: HTMLElement): ComponentReference[] {
  // Find all elements that are components
  const components = [];

  while (element && element.parentNode) {
    const component = getComponent(element);
    if (component.nodeId !== -1) {
      if (component.element?.tagName.startsWith('FLOW-CONTAINER-')) {
        break;
      }
      components.push(component);
    }
    element = element.parentElement ? element.parentElement : ((element.parentNode as ShadowRoot).host as HTMLElement);
  }
  return components.reverse();
}

export type ComponentReference = {
  nodeId: number;
  appId: string;
  element?: HTMLElement;
};

function getComponent(element: HTMLElement): ComponentReference {
  const { clients } = (window as any).Vaadin.Flow;
  const appIds = Object.keys(clients);
  for (const appId of appIds) {
    const client = clients[appId];
    if (client.getNodeId) {
      const nodeId = client.getNodeId(element);
      if (nodeId >= 0) {
        return { nodeId, appId, element };
      }
    }
  }
  return { nodeId: -1, appId: '', element: undefined };
}
