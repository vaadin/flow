/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import { resolve } from 'path';
import { existsSync, readFileSync, writeFileSync } from 'fs';

/**
 * Generate the css.generated.js file for css imports which collects all required information.
 *
 * @param {Object} options build options (e.g. prod or dev mode)
 * @returns {string} theme file content
 */
function writeCssFiles(options) {
  const outputFolder = options.frontendGeneratedFolder;
  const cssFilename = 'css.generated.js';

  let cssFileContent = `import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';\n`;
  cssFileContent += `import { webcomponentGlobalCssInjector } from 'Frontend/generated/jar-resources/theme-util.js';\n`;

  cssFileContent += `let needsReloadOnChanges = false;\n`;

  // Don't format as the generated file formatting will get wonky!
  // If targets check that we only register the style parts once, checks exist for global css and component css
  const themeFileApply = `
  let themeRemovers = new WeakMap();
  let targets = [];
  const fontFaceRegex = /(@font-face\\s*{[\\s\\S]*?})/g;

  export const applyCss = (target) => {
    const removers = [];
    if (target !== document) {
      
      webcomponentGlobalCssInjector((css) => {
        removers.push(injectGlobalCss(css, '', target));
        if(fontFaceRegex.test(css)) {
          const fontFaces = Array.from(css.match(fontFaceRegex));
          fontFaces.forEach(fontFace => {
            removers.push(injectGlobalCss(fontFace, '', document));
          });
        }
      });
    }

    if (import.meta.hot) {
      targets.push(new WeakRef(target));
      themeRemovers.set(target, removers);
    }

  }

`;

  cssFileContent += themeFileApply;
  cssFileContent += `
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

  writeIfChanged(resolve(outputFolder, cssFilename), cssFileContent);
}

function writeIfChanged(file, data) {
  if (!existsSync(file) || readFileSync(file, { encoding: 'utf-8' }) !== data) {
    writeFileSync(file, data);
  }
}

export { writeCssFiles };
