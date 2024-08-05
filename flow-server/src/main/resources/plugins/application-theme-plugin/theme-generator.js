/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import { globSync } from 'glob';
import { resolve, basename } from 'path';
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { checkModules } from './theme-copy.js';

// Special folder inside a theme for component themes that go inside the component shadow root
const themeComponentsFolder = 'components';
// The contents of a global CSS file with this name in a theme is always added to
// the document. E.g. @font-face must be in this
const documentCssFilename = 'document.css';
// styles.css is the only entrypoint css file with document.css. Everything else should be imported using css @import
const stylesCssFilename = 'styles.css';

const CSSIMPORT_COMMENT = 'CSSImport end';
const headerImport = `import 'construct-style-sheets-polyfill';
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
function writeThemeFiles(themeFolder, themeName, themeProperties, options) {
  const productionMode = !options.devMode;
  const useDevServerOrInProductionMode = !options.useDevBundle;
  const outputFolder = options.frontendGeneratedFolder;
  const styles = resolve(themeFolder, stylesCssFilename);
  const documentCssFile = resolve(themeFolder, documentCssFilename);
  const autoInjectComponents = themeProperties.autoInjectComponents ?? true;
  const autoInjectGlobalCssImports = themeProperties.autoInjectGlobalCssImports ?? false;
  const globalFilename = 'theme-' + themeName + '.global.generated.js';
  const componentsFilename = 'theme-' + themeName + '.components.generated.js';
  const themeFilename = 'theme-' + themeName + '.generated.js';

  let themeFileContent = headerImport;
  let globalImportContent = '// When this file is imported, global styles are automatically applied\n';
  let componentsFileContent = '';
  var componentsFiles;

  if (autoInjectComponents) {
    componentsFiles = globSync('*.css', {
      cwd: resolve(themeFolder, themeComponentsFolder),
      nodir: true
    });

    if (componentsFiles.length > 0) {
      componentsFileContent +=
        "import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles';\n";
    }
  }

  if (themeProperties.parent) {
    themeFileContent += `import { applyTheme as applyBaseTheme } from './theme-${themeProperties.parent}.generated.js';\n`;
  }

  themeFileContent += `import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';\n`;
  themeFileContent += `import { webcomponentGlobalCssInjector } from 'Frontend/generated/jar-resources/theme-util.js';\n`;
  themeFileContent += `import './${componentsFilename}';\n`;

  themeFileContent += `let needsReloadOnChanges = false;\n`;
  const imports = [];
  const componentCssImports = [];
  const globalFileContent = [];
  const globalCssCode = [];
  const shadowOnlyCss = [];
  const componentCssCode = [];
  const parentTheme = themeProperties.parent ? 'applyBaseTheme(target);\n' : '';
  const parentThemeGlobalImport = themeProperties.parent
    ? `import './theme-${themeProperties.parent}.global.generated.js';\n`
    : '';

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

  /* LUMO */
  const lumoImports = themeProperties.lumoImports || ['color', 'typography'];
  if (lumoImports) {
    lumoImports.forEach((lumoImport) => {
      imports.push(`import { ${lumoImport} } from '@vaadin/vaadin-lumo-styles/${lumoImport}.js';\n`);
      if (lumoImport === 'utility' || lumoImport === 'badge' || lumoImport === 'typography' || lumoImport === 'color') {
        // Inject into main document the same way as other Lumo styles are injected
        // Lumo imports go to the theme global imports file to prevent style leaks
        // when the theme is applied to an embedded component
        globalFileContent.push(`import '@vaadin/vaadin-lumo-styles/${lumoImport}-global.js';\n`);
      }
    });

    lumoImports.forEach((lumoImport) => {
      // Lumo is injected to the document by Lumo itself
      shadowOnlyCss.push(`removers.push(injectGlobalCss(${lumoImport}.cssText, '', target, true));\n`);
    });
  }

  /* Theme */
  if (useDevServerOrInProductionMode) {
    globalFileContent.push(parentThemeGlobalImport);
    globalFileContent.push(`import 'themes/${themeName}/${filename}';\n`);

    imports.push(`import ${variable} from 'themes/${themeName}/${filename}?inline';\n`);
    shadowOnlyCss.push(`removers.push(injectGlobalCss(${variable}.toString(), '', target));\n    `);
  }
  if (existsSync(documentCssFile)) {
    filename = basename(documentCssFile);
    variable = camelCase(filename);

    if (useDevServerOrInProductionMode) {
      globalFileContent.push(`import 'themes/${themeName}/${filename}';\n`);

      imports.push(`import ${variable} from 'themes/${themeName}/${filename}?inline';\n`);
      shadowOnlyCss.push(`removers.push(injectGlobalCss(${variable}.toString(),'', document));\n    `);
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
          "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm/bun i'"
      );
    }
    themeProperties.documentCss.forEach((cssImport) => {
      const variable = 'module' + i++;
      imports.push(`import ${variable} from '${cssImport}?inline';\n`);
      // Due to chrome bug https://bugs.chromium.org/p/chromium/issues/detail?id=336876 font-face will not work
      // inside shadowRoot so we need to inject it there also.
      globalCssCode.push(`if(target !== document) {
        removers.push(injectGlobalCss(${variable}.toString(), '', target));
    }\n    `);
      globalCssCode.push(
        `removers.push(injectGlobalCss(${variable}.toString(), '${CSSIMPORT_COMMENT}', document));\n    `
      );
    });
  }
  if (themeProperties.importCss) {
    const missingModules = checkModules(themeProperties.importCss);
    if (missingModules.length > 0) {
      throw Error(
        "Missing npm modules or files '" +
          missingModules.join("', '") +
          "' for importCss marked in 'theme.json'.\n" +
          "Install or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm/bun i'"
      );
    }
    themeProperties.importCss.forEach((cssPath) => {
      const variable = 'module' + i++;
      globalFileContent.push(`import '${cssPath}';\n`);
      imports.push(`import ${variable} from '${cssPath}?inline';\n`);
      shadowOnlyCss.push(`removers.push(injectGlobalCss(${variable}.toString(), '${CSSIMPORT_COMMENT}', target));\n`);
    });
  }

  if (autoInjectComponents) {
    componentsFiles.forEach((componentCss) => {
      const filename = basename(componentCss);
      const tag = filename.replace('.css', '');
      const variable = camelCase(filename);
      componentCssImports.push(
        `import ${variable} from 'themes/${themeName}/${themeComponentsFolder}/${filename}?inline';\n`
      );
      // Don't format as the generated file formatting will get wonky!
      const componentString = `registerStyles(
        '${tag}',
        unsafeCSS(${variable}.toString())
      );
      `;
      componentCssCode.push(componentString);
    });
  }

  themeFileContent += imports.join('');

  // Don't format as the generated file formatting will get wonky!
  // If targets check that we only register the style parts once, checks exist for global css and component css
  const themeFileApply = `
  let themeRemovers = new WeakMap();
  let targets = [];

  export const applyTheme = (target) => {
    const removers = [];
    if (target !== document) {
      ${shadowOnlyCss.join('')}
      ${autoInjectGlobalCssImports ? `
        webcomponentGlobalCssInjector((css) => {
          removers.push(injectGlobalCss(css, '', target));
        });
        ` : ''}
    }
    ${parentTheme}
    ${globalCssCode.join('')}

    if (import.meta.hot) {
      targets.push(new WeakRef(target));
      themeRemovers.set(target, removers);
    }

  }

`;
  componentsFileContent += `
${componentCssImports.join('')}

if (!document['${componentCssFlag}']) {
  ${componentCssCode.join('')}
  document['${componentCssFlag}'] = true;
}

if (import.meta.hot) {
  import.meta.hot.accept((module) => {
    window.location.reload();
  });
}

`;

  themeFileContent += themeFileApply;
  themeFileContent += `
if (import.meta.hot) {
  import.meta.hot.accept((module) => {

    if (needsReloadOnChanges) {
      window.location.reload();
    } else {
      targets.forEach(targetRef => {
        const target = targetRef.deref();
        if (target) {
          themeRemovers.get(target).forEach(remover => remover())
          module.applyTheme(target);
        }
      })
    }
  });

  import.meta.hot.on('vite:afterUpdate', (update) => {
    document.dispatchEvent(new CustomEvent('vaadin-theme-updated', { detail: update }));
  });
}

`;

  globalImportContent += `
${globalFileContent.join('')}
`;

  writeIfChanged(resolve(outputFolder, globalFilename), globalImportContent);
  writeIfChanged(resolve(outputFolder, themeFilename), themeFileContent);
  writeIfChanged(resolve(outputFolder, componentsFilename), componentsFileContent);
}

function writeIfChanged(file, data) {
  if (!existsSync(file) || readFileSync(file, { encoding: 'utf-8' }) !== data) {
    writeFileSync(file, data);
  }
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

export { writeThemeFiles };
