const EXPORTED_WEB_COMPONENT_SELECTOR = Symbol('exported-web-component-selector');

const globalStyleSheets = new Map();
const exportedWebComponents = new Set();
const exportedWebComponentStyleSheets = new Map();

/**
 * Helper function to add a style sheet to the shadow root of an element.
 */
function addAdoptedStyleSheet(element, styleSheet) {
  const { shadowRoot } = element;
  if (!shadowRoot.adoptedStyleSheets.includes(styleSheet)) {
    shadowRoot.adoptedStyleSheets.push(styleSheet);
  }
}

/**
 * Helper function to remove a style sheet from the shadow root of an element.
 */
function removeAdoptedStyleSheet(element, styleSheet) {
  const { shadowRoot } = element;
  shadowRoot.adoptedStyleSheets = shadowRoot.adoptedStyleSheets.filter((ss) => ss !== styleSheet);
}

export function injectGlobalCSS(id, content) {
  let style = globalStyleSheets.get(id);
  if (!style) {
    style = document.createElement('style');
    globalStyleSheets.set(id, style);
    document.head.appendChild(style);
  }

  style.textContent = content;
}

export function getExportedWebComponentStyleSheets() {
  return exportedWebComponentStyleSheets.values();
}

export function injectExportedWebComponentCSS(id, content, { selector }) {
  let styleSheet = exportedWebComponentStyleSheets.get(id);
  if (!styleSheet) {
    styleSheet = new CSSStyleSheet();
    exportedWebComponentStyleSheets.set(id, styleSheet);
  }

  styleSheet[EXPORTED_WEB_COMPONENT_SELECTOR] = selector;

  // replaceSync will automatically update the stylesheet in
  // all shadow roots that have adopted it without needing to
  // notify each shadow root individually.
  styleSheet.replaceSync(content);

  // Add the stylesheet to the shadow root of existing exported
  // web component instances that match the selector. Note,
  // if the selector is *, the stylesheet will be added to
  // all shadow roots.
  exportedWebComponents.forEach((component) => {
    if (component.matches(selector)) {
      addAdoptedStyleSheet(component, styleSheet);
    } else {
      removeAdoptedStyleSheet(component, styleSheet);
    }
  });
}

export function exportedWebComponentConnected(component) {
  exportedWebComponents.add(component);

  // Add the stylesheet to the shadow root of the component
  // if it matches the selector.
  exportedWebComponentStyleSheets.forEach((styleSheet) => {
    if (component.matches(styleSheet[EXPORTED_WEB_COMPONENT_SELECTOR])) {
      addAdoptedStyleSheet(component, styleSheet);
    }
  });
}

export function exportedWebComponentDisconnected(component) {
  exportedWebComponents.delete(component);

  // Remove all previously added stylesheets from the shadow root.
  exportedWebComponentStyleSheets.forEach((styleSheet) => {
    removeAdoptedStyleSheet(component, styleSheet);
  });
}
