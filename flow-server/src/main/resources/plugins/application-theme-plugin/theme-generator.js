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
const themeFileAlwaysAddToDocument = 'document.css';

const headerImport = `import 'construct-style-sheets-polyfill';
`;

const injectGlobalCssMethod = `
// target: Document | ShadowRoot
export const injectGlobalCss = (css, target) => {
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(css);
  target.adoptedStyleSheets = [...target.adoptedStyleSheets, sheet];
};
`;

/**
 * Generate the [themeName].js file for themeFolder which collects all required information from the folder.
 *
 * @param {string} themeFolder folder of the theme
 * @param {string} themeName name of the handled theme
 * @param {JSON Object} themeProperties content of theme.json
 * @returns {string} theme file content
 */
function generateThemeFile(themeFolder, themeName, themeProperties) {
  const globalFiles = glob.sync('*.css', {
    cwd: themeFolder,
    nodir: true,
  });
  const componentsFiles = glob.sync('*.css', {
    cwd: path.resolve(themeFolder, themeComponentsFolder),
    nodir: true,
  });

  let themeFile = headerImport;

  if(componentsFiles.length > 0){
    themeFile += 'import { css, unsafeCSS, registerStyles } from \'@vaadin/vaadin-themable-mixin/register-styles\';';
  }

  themeFile += injectGlobalCssMethod;

  const imports = [];
  const globalCssCode = [];
  const componentCssCode = [];

  globalFiles.forEach((global) => {
    const filename = path.basename(global);
    const variable = camelCase(filename);
    imports.push(`import ${variable} from './${filename}';\n`);
    if (filename == themeFileAlwaysAddToDocument) {
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), document);\n    `);
    } else {
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n    `);
    }
  });

  let i = 0;
  if (themeProperties.documentCss) {
    const missingModules = checkModules(themeProperties.documentCss);
    if(missingModules.length > 0) {
      throw Error("Missing npm modules or files '" + missingModules.join("', '")
        + "' for documentCss marked in 'theme.json'.\n" +
        "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'");

    }
    themeProperties.documentCss.forEach((cssImport) => {
      const variable = 'module' + i++;
      imports.push(`import ${variable} from '${cssImport}';\n`);
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), target);\n    `);
      globalCssCode.push(`injectGlobalCss(${variable}.toString(), document);\n    `);
    });
  }
  if (themeProperties.importCss) {
    const missingModules = checkModules(themeProperties.importCss);
    if(missingModules.length > 0) {
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

  const themeIdentifier = '_vaadinds_' + themeName + '_';
  const globalCssFlag = themeIdentifier + 'globalCss';
  const componentCssFlag = themeIdentifier + 'componentCss';

  themeFile += imports.join('');
  themeFile += `
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow['${globalCssFlag}'] = window.Vaadin.Flow['${globalCssFlag}'] || [];
`;

// Don't format as the generated file formatting will get wonky!
// If targets check that we only register the style parts once, checks exist for global css and component css
  const themeFileApply = `export const applyTheme = (target) => {
  const injectGlobal = (window.Vaadin.Flow['${globalCssFlag}'].length === 0) || (!window.Vaadin.Flow['${globalCssFlag}'].includes(target) && target !== document);
  if (injectGlobal) {
    ${globalCssCode.join('')}
    window.Vaadin.Flow['${globalCssFlag}'].push(target);
  }
  if (!document['${componentCssFlag}']) {
    ${componentCssCode.join('')}
    document['${componentCssFlag}'] = true;
  }
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
  return str.replace(/(?:^\w|[A-Z]|\b\w)/g, function(word, index) {
    return index === 0 ? word.toLowerCase() : word.toUpperCase();
  }).replace(/\s+/g, '').replace(/\.|\-/g, '');
}

module.exports = generateThemeFile;
