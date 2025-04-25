const STYLESHEET_SELECTOR = Symbol('stylesheet-selector');

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

  return () => {
    globalStyleSheets.delete(id);
    document.head.removeChild(style);
  }
}

export function injectExportedWebComponentCSS(id, content, { selector }) {
  let styleSheet = exportedWebComponentStyleSheets.get(id);
  if (!styleSheet) {
    styleSheet = new CSSStyleSheet();
    exportedWebComponentStyleSheets.set(id, styleSheet);
  }

  styleSheet[STYLESHEET_SELECTOR] = selector;
  styleSheet.replaceSync(content);

  exportedWebComponents.forEach((component) => {
    if (component.matches(selector)) {
      addAdoptedStyleSheet(component, styleSheet);
    } else {
      removeAdoptedStyleSheet(component, styleSheet);
    }
  });

  return () => {
    exportedWebComponentStyleSheets.delete(id);
    exportedWebComponents.forEach((component) => removeAdoptedStyleSheet(component, styleSheet));
  }
}

export function exportedWebComponentConnected(component) {
  exportedWebComponents.add(component);
  exportedWebComponentStyleSheets.forEach((styleSheet) => {
    if (component.matches(styleSheet[STYLESHEET_SELECTOR])) {
      addAdoptedStyleSheet(component, styleSheet);
    }
  })
}

export function exportedWebComponentDisconnected(component) {
  exportedWebComponents.delete(component);
  exportedWebComponentStyleSheets.forEach((styleSheet) => {
    removeAdoptedStyleSheet(component, styleSheet);
  });
}
