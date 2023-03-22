function getAllStylesheets() {
  const result: CSSStyleSheet[] = [];

  function addImportedSheets(stylesheet: CSSStyleSheet) {
    [...stylesheet.cssRules].forEach((rule) => {
      if (rule instanceof CSSImportRule && rule.styleSheet) {
        result.push(rule.styleSheet);
        addImportedSheets(rule.styleSheet);
      }
    });
  }

  [...document.styleSheets, ...document.adoptedStyleSheets].forEach((sheet) => {
    result.push(sheet);
    addImportedSheets(sheet);
  });

  return result;
}

function getUniqueClassNames() {
  const result = new Set<string>();
  const stylesheets = getAllStylesheets();

  stylesheets.forEach((stylesheet) => {
    // Add guard in case stylesheet is not accessible
    try {
      [...stylesheet.cssRules].forEach((rule) => {
        const matches = rule.cssText.match(/\.[\w\d-_]+/g);
        if (matches) {
          matches.forEach((className) => result.add(className));
        }
      });
    } catch (e) {
      // Ignore
    }
  });

  return result;
}

export function suggestUniqueClassName(element: HTMLElement) {
  const tagName = element.localName;
  const sanitizedName = tagName.replace('vaadin-', '');
  const existingClassnames = getUniqueClassNames();

  let counter = 1;
  while (true) {
    const suggestedName = `${sanitizedName}-${counter}`;
    const suggestedNameWithDot = `.${suggestedName}`;
    if (!existingClassnames.has(suggestedNameWithDot)) {
      return suggestedName;
    }
    counter++;
  }
}
