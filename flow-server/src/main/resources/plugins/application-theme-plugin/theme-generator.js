/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * This file handles the generation of the '[theme-name].js' to
 * the themes/[theme-name] folder according to properties from 'theme.json'.
 */
const glob = require('glob');
const path = require('path');
const fs = require('fs');
const { checkModules } = require('./theme-copy');

// Special folder inside a theme for component themes that go inside the component shadow root
const themeComponentsFolder = 'components';
// The contents of a global CSS file with this name in a theme is always added to
// the document. E.g. @font-face must be in this
const documentCssFile = 'document.css';
// styles.css is the only entrypoint css file with document.css. Everything else should be imported using css @import
const stylesCssFile = 'styles.css';

const headerImport = `import 'construct-style-sheets-polyfill';
import { DomModule } from "@polymer/polymer/lib/elements/dom-module";
import { stylesFromTemplate } from "@polymer/polymer/lib/utils/style-gather";
`;

const getStyleModule = `
const getStyleModule = (id) => {
  const template = DomModule.import(id, "template");
  const cssText =
    template &&
    stylesFromTemplate(template, "")
      .map((style) => style.textContent)
      .join(" ");
  return cssText;
};
`;

const createLinkReferences = `
const createLinkReferences = (css, target) => {
  // Unresolved urls are written as '@import url(text);' to the css
  // [0] is the full match
  // [1] matches the media query
  // [2] matches the url
  const importMatcher = /(?:@media\\s(.+?))?(?:\\s{)?\\@import\\surl\\((.+?)\\);(?:})?/g;
  
  var match;
  var styleCss = css;
  
  // For each external url import add a link reference
  while((match = importMatcher.exec(css)) !== null) {
    styleCss = styleCss.replace(match[0], "");
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = match[2];
    if (match[1]) {
      link.media = match[1];
    }
    // For target document append to head else append to target
    if (target === document) {
      document.head.appendChild(link);
    } else {
      target.appendChild(link);
    }
  };
  return styleCss;
};
`;

const injectGlobalCssMethod = `
// target: Document | ShadowRoot
export const injectGlobalCss = (css, target, first) => {
  
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(createLinkReferences(css,target));
  if (first) {
    target.adoptedStyleSheets = [sheet, ...target.adoptedStyleSheets];
  } else {
    target.adoptedStyleSheets = [...target.adoptedStyleSheets, sheet];
  }
};
`;

const addCssBlockMethod = `
const addCssBlock = function (block, before = false) {
  const tpl = document.createElement("template");
  tpl.innerHTML = block;
  document.head[before ? "insertBefore" : "appendChild"](
    tpl.content,
    document.head.firstChild
  );
};
`;

const addStyleIncludeMethod = `
const addStyleInclude = (module, target) => {
  addCssBlock(\`<custom-style><style include="\${module}"></style></custom-style>\`, true);
};
`;

/**
 * Generate the [themeName].js file for themeFolder which collects all required information from the folder.
 *
 * @param {string} themeFolder folder of the theme
 * @param {string} themeName name of the handled theme
 * @param {JSON} themeProperties content of theme.json
 * @param {boolean} productionMode true if making a production build.
 * @returns {string} theme file content
 */
function generateThemeFile(themeFolder, themeName, themeProperties, productionMode) {
  const styles = path.resolve(themeFolder, stylesCssFile);
  const document = path.resolve(themeFolder, documentCssFile);
  const componentsFiles = glob.sync('*.css', {
    cwd: path.resolve(themeFolder, themeComponentsFolder),
    nodir: true,
  });

  let themeFile = headerImport;

  if (componentsFiles.length > 0) {
    themeFile += 'import { css, unsafeCSS, registerStyles } from \'@vaadin/vaadin-themable-mixin/register-styles\';\n';
  }

  if (themeProperties.parent) {
    themeFile += `import {applyTheme as applyBaseTheme} from 'themes/${themeProperties.parent}/${themeProperties.parent}.generated.js';`;
  }

  themeFile += createLinkReferences;
  themeFile += injectGlobalCssMethod;
  themeFile += addCssBlockMethod;
  themeFile += addStyleIncludeMethod;
  themeFile += getStyleModule;

  const imports = [];
  const globalCssCode = [];
  const lumoCssCode = [];
  const componentCssCode = [];
  const parentTheme = themeProperties.parent ? 'applyBaseTheme(target);\n' : '';

  const themeIdentifier = '_vaadintheme_' + themeName + '_';
  const lumoCssFlag = '_vaadinthemelumoimports_';
  const globalCssFlag = themeIdentifier + 'globalCss';
  const componentCssFlag = themeIdentifier + 'componentCss';

  if (!fs.existsSync(styles)) {
    if (productionMode) {
      throw new Error(`styles.css file is missing and is needed for '${themeName}' in folder '${themeFolder}'`);
    }
    fs.writeFileSync(styles, "/* Import your application global css files here or add the styles directly to this file */", "utf8");
  }

  // styles.css will always be available as we write one if it doesn't exist.
  let filename = path.basename(styles);
  let variable = camelCase(filename);
  imports.push(`import ${variable} from './${filename}';\n`);

  /* Lumo must be first so that custom styles override Lumo styles */
  const lumoImports = themeProperties.lumoImports || ["color", "typography"];
  if (lumoImports && lumoImports.length > 0) {
    lumoImports.forEach((lumoImport) => {
      imports.push(`import '@vaadin/vaadin-lumo-styles/${lumoImport}.js';\n`);
    });

    lumoCssCode.push(`// Lumo styles are injected into shadow roots.\n`)
    lumoCssCode.push(`// For the document, we need to be compatible with flow-generated-imports and add missing <style> tags.\n`)
    lumoCssCode.push(`const shadowRoot = (target instanceof ShadowRoot);\n`)
    lumoCssCode.push(`if (shadowRoot) {\n`);
    lumoImports.forEach((lumoImport) => {
      lumoCssCode.push(`injectGlobalCss(getStyleModule("lumo-${lumoImport}"), target, true);\n`);
    });

    lumoCssCode.push(`} else if (!document['${lumoCssFlag}']) {\n`);
    lumoImports.forEach((lumoImport) => {
      lumoCssCode.push(`addStyleInclude("lumo-${lumoImport}", target);\n`);
    });
    lumoCssCode.push(`document['${lumoCssFlag}'] = true;\n`);
    lumoCssCode.push(`}\n`);
  }

  globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n    `);
  if (fs.existsSync(document)) {
    filename = path.basename(document);
    variable = camelCase(filename);
    imports.push(`import ${variable} from './${filename}';\n`);
    globalCssCode.push(`injectGlobalCss(${variable}.toString(), document);\n    `);
  }

  let i = 0;
  if (themeProperties.documentCss) {
    const missingModules = checkModules(themeProperties.documentCss);
    if (missingModules.length > 0) {
      throw Error("Missing npm modules or files '" + missingModules.join("', '")
        + "' for documentCss marked in 'theme.json'.\n" +
        "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'");

    }
    themeProperties.documentCss.forEach((cssImport) => {
      const variable = 'module' + i++;
      imports.push(`import ${variable} from '${cssImport}';\n`);
      // Due to chrome bug https://bugs.chromium.org/p/chromium/issues/detail?id=336876 font-face will not work
      // inside shadowRoot so we need to inject it there also.
      globalCssCode.push(`if(target !== document) {
      injectGlobalCss(${variable}.toString(), target);
    }\n    `);
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), document);\n    `);
    });
  }
  if (themeProperties.importCss) {
    const missingModules = checkModules(themeProperties.importCss);
    if (missingModules.length > 0) {
      throw Error("Missing npm modules or files '" + missingModules.join("', '")
        + "' for importCss marked in 'theme.json'.\n" +
        "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'");

    }
    themeProperties.importCss.forEach((cssPath) => {
      const variable = 'module' + i++;
      imports.push(`import ${variable} from '${cssPath}';\n`);
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n`);
    });
  }

  componentsFiles.forEach((componentCss) => {
    const filename = path.basename(componentCss);
    const tag = filename.replace('.css', '');
    const variable = camelCase(filename);
    imports.push(
      `import ${variable} from './${themeComponentsFolder}/${filename}';\n`
    );
// Don't format as the generated file formatting will get wonky!
    const componentString = `registerStyles(
      '${tag}',
      css\`
        \${unsafeCSS(${variable}.toString())}
      \`
    );
    `;
    componentCssCode.push(componentString);
  });

  themeFile += imports.join('');
  themeFile += `
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow['${globalCssFlag}'] = window.Vaadin.Flow['${globalCssFlag}'] || [];
`;

// Don't format as the generated file formatting will get wonky!
// If targets check that we only register the style parts once, checks exist for global css and component css
  const themeFileApply = `export const applyTheme = (target) => {
  ${parentTheme}
  const injectGlobal = (window.Vaadin.Flow['${globalCssFlag}'].length === 0) || (!window.Vaadin.Flow['${globalCssFlag}'].includes(target) && target !== document);
  if (injectGlobal) {
    ${globalCssCode.join('')}
    window.Vaadin.Flow['${globalCssFlag}'].push(target);
  }
  if (!document['${componentCssFlag}']) {
    ${componentCssCode.join('')}
    document['${componentCssFlag}'] = true;
  }
  ${lumoCssCode.join('')}
}
`;

  themeFile += themeFileApply;

  return themeFile;
};

/**
 * Make given string into camelCase.
 *
 * @param {string} str string to make into cameCase
 * @returns {string} camelCased version
 */
function camelCase(str) {
  return str.replace(/(?:^\w|[A-Z]|\b\w)/g, function (word, index) {
    return index === 0 ? word.toLowerCase() : word.toUpperCase();
  }).replace(/\s+/g, '').replace(/\.|\-/g, '');
}

module.exports = generateThemeFile;
