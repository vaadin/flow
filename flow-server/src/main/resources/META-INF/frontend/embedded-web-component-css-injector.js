const embeddedWebComponentCSS = new Set();
const embeddedWebComponentCSSStyleSheet = new CSSStyleSheet();

export function injectEmbeddedWebComponentCSS(cssText) {
  embeddedWebComponentCSS.add(cssText);
  embeddedWebComponentCSSStyleSheet.replaceSync([...embeddedWebComponentCSS].join('\n'));
}

export function embeddedWebComponentConnected(component) {
  component.shadowRoot.adoptedStyleSheets.unshift(embeddedWebComponentCSSStyleSheet);
}

export function embeddedWebComponentDisconnected(component) {
  component.shadowRoot.adoptedStyleSheets = component.shadowRoot.adoptedStyleSheets.filter((styleSheet) => {
    return styleSheet !== embeddedWebComponentCSSStyleSheet;
  });
}
