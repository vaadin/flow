/*
 * Copyright 2000-2018 Vaadin Ltd.
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
"use strict";

const gulp = require('gulp');
const fs = require('fs-extra');
const globalVar = require('./lib/js/global-variables');
const ElementFilter = require('./lib/js/element-filter');
const VersionReader = require('./lib/js/version-transform');
const MixinCollector = require('./lib/js/mixin-collector');
const AnalyzerTransform = require('./lib/js/analyzer-transform');
const ElementJsonTransform = require('./lib/js/element-json-transform');
const gulpIgnore = require('gulp-ignore');
const cheerio = require('gulp-cheerio');

const variantsData = {};

gulp.task('prepare', cb => {
  if (!fs.existsSync(globalVar.bowerSrcDir) || fs.readdirSync(globalVar.bowerSrcDir).length === 0) {
    console.error(`Source directory ${globalVar.bowerSrcDir} does not exists or empty`);
    process.exit(1)
  }

  console.log(`Cleaning output directory ${globalVar.targetDir}`);
  fs.removeSync(globalVar.targetDir);
  cb();
});

gulp.task('gather-variants-data', ['prepare'], () => {
  function extractVariants(text) {
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

  const modulesData = {};
  const themeToTagToModuleId = {};

  return gulp.src('/Users/someonetoignore/Work/components/*/theme/*/vaadin-*-styles.html')
    .pipe(cheerio(($, styleFile, cb) => {
      $('dom-module').each((_, domModuleElement) => {
        const themeName = (styleFile.path.match(/theme\/([^\/]+)\//) || [])[1];
        if (!themeName) {
          return cb(new Error(`Failed to find a theme for path '${styleFile.path}'`));
        }

        const themeModules = modulesData[themeName] || (modulesData[themeName] = {});
        const moduleId = domModuleElement.attribs['id'];
        if (themeModules[moduleId]) {
          throw new Error(`Have found multiple 'dom-module' element declarations with the same id: '${moduleId}'. File with the second declaration: '${styleFile.path}'`);
        }

        const domModuleSelector = $(domModuleElement);
        const variants = extractVariants(domModuleSelector.text());
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
            const componentThemes = (variantsData[componentTag] || (variantsData[componentTag] = {}));
            const componentVariants = (componentThemes[themeName] || (componentThemes[themeName] = new Set()));
            if (variants.size) {
              variants.forEach(variant => componentVariants.add(variant));
            }
          }
        }
        themeModules[moduleId] = {dependencies, variants, componentTag};
      });
      cb();
    }))
    .on('finish', () => {
      for (const componentTag in variantsData) {
        const componentThemes = variantsData[componentTag];
        for (const themeName in componentThemes) {
          const themeModules = modulesData[themeName];
          const moduleId = themeToTagToModuleId[themeName][componentTag];
          const variantsToFill = componentThemes[themeName];
          fillVariants(variantsToFill, themeModules, (themeModules[moduleId] || {}).dependencies);
          if (!variantsToFill.size) {
            delete componentThemes[themeName];
          } else {
            componentThemes[themeName] = [...variantsToFill];
          }
        }
        if (!Object.keys(componentThemes).length) {
          delete variantsData[componentTag];
        }
      }
    });
});

function fillVariants(variantsToFill, modules, dependencies) {
  if (dependencies && dependencies.length) {
    for (const dependency of dependencies) {
      const dependencyData = modules[dependency];
      if (dependencyData) {
        (dependencyData.variants || []).forEach(variant => variantsToFill.add(variant));
        fillVariants(variantsToFill, modules, dependencyData.dependencies);
      }
    }
  }
}

gulp.task('generate', ['gather-variants-data'], () => {
  console.log(`Running generate task, for resources from: ${globalVar.bowerSrcDir}`);
  // the element filter reads the bower.json file and parses the dependencies
  const elementFilter = new ElementFilter();
  // the version reader reads the versions only for those elements that the json is created for
  const versionReader = new VersionReader();
  // stores mixin/behavior information of encountered components
  const mixinCollector = new MixinCollector();

  return gulp.src([globalVar.bowerSrcDir + "/*/*.html",
    // ignore Polymer itself
    "!" + globalVar.bowerSrcDir + "polymer/*",
    // ignore all demo.html, index.html and metadata.html files
    "!" + globalVar.bowerSrcDir + "*/demo.html",
    "!" + globalVar.bowerSrcDir + "*/index.html",
    "!" + globalVar.bowerSrcDir + "*/metadata.html",
    // includes a set of js files only, and some do not exist
    "!" + globalVar.bowerSrcDir + "web-animations-js/*",
    // Not useful in gwt and also has spurious event names
    "!" + globalVar.bowerSrcDir + "iron-jsonp-library/*",
  ])
    .pipe(gulpIgnore.include(file => elementFilter.acceptFile(file))) // ignores files not directly mentioned in the dependencies
    .pipe(versionReader) // Reads the versions of the elements
    .pipe(new AnalyzerTransform(elementFilter, mixinCollector)) // transforms out PolymerElements
    .pipe(new ElementJsonTransform(versionReader, mixinCollector, variantsData)) // transforms out json files
    .pipe(gulp.dest('.'));
});

gulp.task('default', ['generate']);
