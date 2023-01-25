export type ComponentReference = {
  nodeId: number;
  uiId: number;
  element?: HTMLElement;
};

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

function getComponent(element: HTMLElement): ComponentReference {
  const vaadin = (window as any).Vaadin;
  if (vaadin && vaadin.Flow) {
    const { clients } = vaadin.Flow;
    const appIds = Object.keys(clients);
    for (const appId of appIds) {
      const client = clients[appId];
      if (client.getNodeId) {
        const nodeId = client.getNodeId(element);
        if (nodeId >= 0) {
          return { nodeId, uiId: client.getUIId(), element };
        }
      }
    }
  }
  return { nodeId: -1, uiId: -1, element: undefined };
}
