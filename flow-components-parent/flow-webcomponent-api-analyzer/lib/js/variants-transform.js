/*
 * Copyright 2000-2019 Vaadin Ltd.
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
'use strict';

const {Transform} = require('readable-stream');
const cheerio = require('cheerio');

function _extractVariants(text) {
  const variantsRegex = /\[theme~=["|']([^'"]+)["|']/ig;
  const variants = new Set();

  let matches;
  while ((matches = variantsRegex.exec(text))) {
    const newVariant = matches[1];
    if (newVariant) {
      variants.add(newVariant);
    }
  }
  return variants;
}

function _fillVariantsFromDependencies(variantsToFill, modules, dependencyIds) {
  if (dependencyIds && dependencyIds.length) {
    for (const dependencyId of dependencyIds) {
      const dependencyData = modules[dependencyId];
      if (dependencyData) {
        (dependencyData.variants || []).forEach(variant => variantsToFill.add(variant));
        _fillVariantsFromDependencies(variantsToFill, modules, dependencyData.dependencies);
      } else {
        // If we would like to have a complete dependency tree, lumo and material packages need to be analyzed also.
        // Those packages do not contain theme declarations currently and addin them into the current project
        // is really complicated, so simple logging is preferred currently.
        console.debug(`Module data for module id '${dependencyId}' is missing. Ignoring its dependencies.`);
      }
    }
  }
}

const modulesData = {};
const themeToTagToModuleId = {};

module.exports = class VariantsTransform extends Transform {
  /**
   * Fills `variantsData` object with variants from the files parsed.
   *
   * @param variantsData the object to fill with variants parsed
   */
  constructor(variantsData) {
    super({objectMode: true});
    if (!variantsData || typeof variantsData !== 'object') {
      throw new Error(`Expected constructor parameter to be non-null object but got: '${variantsData}'`);
    }
    this.variantsData = variantsData;
  }

  /**
   * First step, where style file is processed.
   * Style files are investigated for `dom-module` presence.
   * Each of the `dom-module` tag contents is parsed and the data required is extracted:
   * * `id` attribute value – module id
   * * `theme-for` attribute value – tag name of the component the theme is applicable to
   * * [theme~="small"] theme variants that are present in the theme
   * * style tags with `include` attribute are retrieved.
   * The value of the attribute is a whitespace-separated `dom-module` ids that are looked for variants
   * (including their style tags with `include` attributes, recursively).
   *
   * After the module data is parsed:
   * * each module data is saved in the map under its id (`modulesData`)
   * * if present, relation between component tag and module id is saved for each theme name (`themeToTagToModuleId`)
   *
   * Whenever possible, `variantsData` is populated with the data.
   */
  _transform(styleFile, enc, cb) {
    const $ = cheerio.load(styleFile.contents.toString(enc));

    $('dom-module').each((_, domModuleElement) => {
      // for every line that has theme word, followed by either backward or forward slash, 
      // then any sequence of non-slash symbols, ending with either forward or backward slash 
      // match the sequence of non-slash symbols
      const themeName = (styleFile.path.match(/theme[\/|\\]([^\/\\]+)[\/|\\]/) || [])[1];

      if (!themeName) {
        return cb(new Error(`Failed to find a theme for path '${styleFile.path}'`));
      }

      const themeModules = modulesData[themeName] || (modulesData[themeName] = {});
      const moduleId = domModuleElement.attribs['id'];
      if (themeModules[moduleId]) {
        throw new Error(`Have found multiple 'dom-module' element declarations with the same id: '${moduleId}'. File with the second declaration: '${styleFile.path}'`);
      }

      const domModuleSelector = $(domModuleElement);
      const variants = _extractVariants(domModuleSelector.text());
      const dependencies = domModuleSelector
        .find('style[include]')
        .map((i, styleElement) => {
          const includeAttributeValue = styleElement.attribs['include'];
          if (includeAttributeValue) {
            return includeAttributeValue.split(' ');
          }
        })
        .get();
      const componentTag = domModuleElement.attribs['theme-for'];
      if (componentTag) {
        const tagToModuleId = themeToTagToModuleId[themeName] || (themeToTagToModuleId[themeName] = {});
        tagToModuleId[componentTag] = moduleId;
        if (variants.size || dependencies.length) {
          const componentThemes = (this.variantsData[componentTag] || (this.variantsData[componentTag] = {}));
          const componentVariants = (componentThemes[themeName] || (componentThemes[themeName] = new Set()));
          if (variants.size) {
            variants.forEach(variant => componentVariants.add(variant));
          }
        }
      }
      themeModules[moduleId] = {dependencies, variants, componentTag};
    });
    cb();
  }

  /**
   * After all modules are parsed and the supplementary data is collected,
   * `variantsData` is additionally filled with all dependencies' variants, recursively.
   */
  _flush(cb) {
    for (const componentTag in this.variantsData) {
      if (!this.variantsData.hasOwnProperty(componentTag)) {
        continue;
      }

      const componentThemes = this.variantsData[componentTag];
      var themeNames = [];
      for (const themeName in componentThemes) {
        if (!componentThemes.hasOwnProperty(themeName)) {
          continue;
        }
        themeNames.push(themeName);

        const themeModules = modulesData[themeName];
        const moduleId = themeToTagToModuleId[themeName][componentTag];
        const variantsToFill = componentThemes[themeName];
        _fillVariantsFromDependencies(variantsToFill, themeModules, (themeModules[moduleId] || {}).dependencies);
        if (!variantsToFill.size) {
          delete componentThemes[themeName];
        } else {
          componentThemes[themeName] = [...variantsToFill];
        }
      }
      if (Object.keys(componentThemes).length) {
          themeNames = themeNames.sort();
          const sortedThemes = {};
          themeNames.forEach( theme => sortedThemes[theme] = componentThemes[theme]);
          this.variantsData[componentTag] = sortedThemes;  
      }
      else {
          delete this.variantsData[componentTag];
      }
    }
    cb();
  }
};
