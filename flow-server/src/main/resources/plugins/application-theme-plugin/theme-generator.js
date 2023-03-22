/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import glob from 'glob';
import { resolve, basename } from 'path';
import { existsSync, writeFileSync } from 'fs';
import { checkModules } from './theme-copy.js';

const { sync } = glob;

// Special folder inside a theme for component themes that go inside the component shadow root
const themeComponentsFolder = 'components';
// The contents of a global CSS file with this name in a theme is always added to
// the document. E.g. @font-face must be in this
const documentCssFile = 'document.css';
// styles.css is the only entrypoint css file with document.css. Everything else should be imported using css @import
const stylesCssFile = 'styles.css';

const headerImport = `import 'construct-style-sheets-polyfill';
`;

const createLinkReferences = `
const createLinkReferences = (css, target) => {
  // Unresolved urls are written as '@import url(text);' or '@import "text";' to the css
  // media query can be present on @media tag or on @import directive after url
  // Note that with Vite production build there is no space between @import and "text"
  // [0] is the full match
  // [1] matches the media query
  // [2] matches the url
  // [3] matches the quote char surrounding in '@import "..."'
  // [4] matches the url in '@import "..."'
  // [5] matches media query on @import statement
  const importMatcher = /(?:@media\\s(.+?))?(?:\\s{)?\\@import\\s*(?:url\\(\\s*['"]?(.+?)['"]?\\s*\\)|(["'])((?:\\\\.|[^\\\\])*?)\\3)([^;]*);(?:})?/g
  
  // Only cleanup if comment exist
  if(/\\/\\*(.|[\\r\\n])*?\\*\\//gm.exec(css) != null) {
    // clean up comments
    css = stripCssComments(css);
  }
  
  var match;
  var styleCss = css;
  
  // For each external url import add a link reference
  while((match = importMatcher.exec(css)) !== null) {
    styleCss = styleCss.replace(match[0], "");
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = match[2] || match[4];
    const media = match[1] || match[5];
    if (media) {
      link.media = media;
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
  if(target === document) {
    const hash = getHash(css);
    if (window.Vaadin.theme.injectedGlobalCss.indexOf(hash) !== -1) {
      return;
    }
    window.Vaadin.theme.injectedGlobalCss.push(hash);
  }
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(createLinkReferences(css,target));
  if (first) {
    target.adoptedStyleSheets = [sheet, ...target.adoptedStyleSheets];
  } else {
    target.adoptedStyleSheets = [...target.adoptedStyleSheets, sheet];
  }
};
`;

const addLumoImportStyleTag = `
const addLumoImportStyleTag = (lumoStyles, target) => {
  const styleTag = document.createElement('style');
  styleTag.type = 'text/css';
  styleTag.appendChild(document.createTextNode(lumoStyles));
  // For target document append to head else append to target
  if (target === document) {
    document.head.appendChild(styleTag);
  } else {
    target.appendChild(styleTag);
  }
};
`;

/**
 * Generate the [themeName].js file for themeFolder which collects all required information from the folder.
 *
 * @param {string} themeFolder folder of the theme
 * @param {string} themeName name of the handled theme
 * @param {JSON} themeProperties content of theme.json
 * @param {Object} options build options (e.g. prod or dev mode)
 * @returns {string} theme file content
 */
function generateThemeFile(themeFolder, themeName, themeProperties, options) {
  const productionMode = !options.devMode;
  const useDevServer = !options.useDevBundle;
  const styles = resolve(themeFolder, stylesCssFile);
  const document = resolve(themeFolder, documentCssFile);
  const autoInjectComponents = themeProperties.autoInjectComponents ?? true;
  let themeFile = headerImport;
  var componentsFiles;

  if (autoInjectComponents) {
    componentsFiles = sync('*.css', {
      cwd: resolve(themeFolder, themeComponentsFolder),
      nodir: true
    });

    if (componentsFiles.length > 0) {
      themeFile += "import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles';\n";
    }
  }

  if (themeProperties.parent) {
    themeFile += `import {applyTheme as applyBaseTheme} from './theme-${themeProperties.parent}.generated.js';\n`;
  }
  themeFile += `import stripCssComments from 'strip-css-comments';\n`;

  themeFile += createLinkReferences;
  themeFile += injectGlobalCssMethod;
  themeFile += addLumoImportStyleTag;

  const imports = [];
  const globalCssCode = [];
  const lumoCssCode = [];
  const componentCssCode = [];
  const parentTheme = themeProperties.parent ? 'applyBaseTheme(target);\n' : '';

  const themeIdentifier = '_vaadintheme_' + themeName + '_';
  const lumoCssFlag = '_vaadinthemelumoimports_';
  const globalCssFlag = themeIdentifier + 'globalCss';
  const componentCssFlag = themeIdentifier + 'componentCss';

  if (!existsSync(styles)) {
    if (productionMode) {
      throw new Error(`styles.css file is missing and is needed for '${themeName}' in folder '${themeFolder}'`);
    }
    writeFileSync(
      styles,
      '/* Import your application global css files here or add the styles directly to this file */',
      'utf8'
    );
  }

  // styles.css will always be available as we write one if it doesn't exist.
  let filename = basename(styles);
  let variable = camelCase(filename);
  if (useDevServer) {
    imports.push(`import ${variable} from 'themes/${themeName}/${filename}?inline';\n`);
  }
  /* Lumo must be first so that custom styles override Lumo styles */
  const lumoImports = themeProperties.lumoImports || ['color', 'typography'];
  if (lumoImports && lumoImports.length > 0) {
    lumoImports.forEach((lumoImport) => {
      imports.push(`import { ${lumoImport} } from '@vaadin/vaadin-lumo-styles/${lumoImport}.js';\n`);
    });

    if (useDevServer) {
      lumoImports.forEach((lumoImport) => {
        lumoCssCode.push(`injectGlobalCss(${lumoImport}.cssText, target, true);\n`);
      });
    } else {
      lumoImports.forEach((lumoImport) => {
        lumoCssCode.push(`addLumoImportStyleTag(${lumoImport}.cssText, target);\n`);
      });
    }
  }

  if (useDevServer) {
    globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n    `);
  }
  if (existsSync(document)) {
    filename = basename(document);
    variable = camelCase(filename);

    if (useDevServer) {
      imports.push(`import ${variable} from 'themes/${themeName}/${filename}?inline';\n`);
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), document);\n    `);
    }
  }

  let i = 0;
  if (themeProperties.documentCss) {
    const missingModules = checkModules(themeProperties.documentCss);
    if (missingModules.length > 0) {
      throw Error(
        "Missing npm modules or files '" +
          missingModules.join("', '") +
          "' for documentCss marked in 'theme.json'.\n" +
          "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'"
      );
    }
    themeProperties.documentCss.forEach((cssImport) => {
      const variable = 'module' + i++;
      imports.push(`import ${variable} from '${cssImport}?inline';\n`);
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
      throw Error(
        "Missing npm modules or files '" +
          missingModules.join("', '") +
          "' for importCss marked in 'theme.json'.\n" +
          "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'"
      );
    }
    themeProperties.importCss.forEach((cssPath) => {
      const variable = 'module' + i++;
      imports.push(`import ${variable} from '${cssPath}';\n`);
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n`);
    });
  }

  if (autoInjectComponents) {
    componentsFiles.forEach((componentCss) => {
      const filename = basename(componentCss);
      const tag = filename.replace('.css', '');
      const variable = camelCase(filename);
      imports.push(`import ${variable} from 'themes/${themeName}/${themeComponentsFolder}/${filename}?inline';\n`);
      // Don't format as the generated file formatting will get wonky!
      const componentString = `registerStyles(
        '${tag}',
        unsafeCSS(${variable}.toString())
      );
      `;
      componentCssCode.push(componentString);
    });
  }

  themeFile += imports.join('');
  themeFile += `
window.Vaadin = window.Vaadin || {};
window.Vaadin.theme = window.Vaadin.theme || {};
window.Vaadin.theme.injectedGlobalCss = [];

/**
 * Calculate a 32 bit FNV-1a hash
 * Found here: https://gist.github.com/vaiorabbit/5657561
 * Ref.: http://isthe.com/chongo/tech/comp/fnv/
 *
 * @param {string} str the input value
 * @returns {string} 32 bit (as 8 byte hex string)
 */
function hashFnv32a(str) {
  /*jshint bitwise:false */
  let i, l, hval = 0x811c9dc5;

  for (i = 0, l = str.length; i < l; i++) {
    hval ^= str.charCodeAt(i);
    hval += (hval << 1) + (hval << 4) + (hval << 7) + (hval << 8) + (hval << 24);
  }

  // Convert to 8 digit hex string
  return ("0000000" + (hval >>> 0).toString(16)).substr(-8);
}

/**
 * Calculate a 64 bit hash for the given input.
 * Double hash is used to significantly lower the collision probability.
 *
 * @param {string} input value to get hash for
 * @returns {string} 64 bit (as 16 byte hex string)
 */
function getHash(input) {
  let h1 = hashFnv32a(input); // returns 32 bit (as 8 byte hex string)
  return h1 + hashFnv32a(h1 + input); 
}
`;

  // Don't format as the generated file formatting will get wonky!
  // If targets check that we only register the style parts once, checks exist for global css and component css
  const themeFileApply = `export const applyTheme = (target) => {
  ${parentTheme}
  ${globalCssCode.join('')}
  
  if (!document['${componentCssFlag}']) {
    ${componentCssCode.join('')}
    document['${componentCssFlag}'] = true;
  }
  ${lumoCssCode.join('')}
}
`;

  themeFile += themeFileApply;

  return themeFile;
}

/**
 * Make given string into camelCase.
 *
 * @param {string} str string to make into cameCase
 * @returns {string} camelCased version
 */
function camelCase(str) {
  return str
    .replace(/(?:^\w|[A-Z]|\b\w)/g, function (word, index) {
      return index === 0 ? word.toLowerCase() : word.toUpperCase();
    })
    .replace(/\s+/g, '')
    .replace(/\.|\-/g, '');
}

export { generateThemeFile };
