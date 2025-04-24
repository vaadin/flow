const globalStyleSheetsMap = new Map();
const exportedWebComponents = new Set();
const exportedWebComponentStyleSheetsMap = new Map();

const EXPORTED_WEB_COMPONENT_STYLE_SHEET = Symbol('exported-web-component-style-sheet');

export function injectGlobalStyles(id, content) {
  let style = globalStyleSheetsMap.get(id);
  if (!style) {
    style = document.createElement('style');
    globalStyleSheetsMap.set(id, style);
  }

  style.textContent = content;
  document.head.appendChild(style);
}

export function injectExportedWebComponentStyles(id, content) {
  let styleSheet = exportedWebComponentStyleSheetsMap.get(id);
  if (!styleSheet) {
    styleSheet = new CSSStyleSheet();
    styleSheet[EXPORTED_WEB_COMPONENT_STYLE_SHEET] = true;

    exportedWebComponentStyleSheetsMap.set(id, styleSheet);
    exportedWebComponents.forEach((component) => {
      component.shadowRoot.adoptedStyleSheets.push(styleSheet);
    });
  }

  styleSheet.replaceSync(content);
}

export function exportedWebComponentConnected(component) {
  exportedWebComponents.add(component);

  component.shadowRoot.adoptedStyleSheets.push(...exportedWebComponentStyleSheetsMap.values());
}

export function exportedWebComponentDisconnected(component) {
  exportedWebComponents.delete(component);

  component.shadowRoot.adoptedStyleSheets = component.shadowRoot.adoptedStyleSheets
    .filter((styleSheet) => !styleSheet[EXPORTED_WEB_COMPONENT_STYLE_SHEET]);
}
