/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

import {assert} from 'chai';

import {applyBuildPreset, isValidPreset, ProjectBuildOptions} from '../builds';

suite('builds', () => {
  suite('isValidPreset()', () => {
    test('returns true for valid presets', () => {
      assert.equal(isValidPreset('es5-bundled'), true);
      assert.equal(isValidPreset('es6-bundled'), true);
      assert.equal(isValidPreset('es6-unbundled'), true);
    });

    test('returns false for a selection of invalid presets', () => {
      assert.equal(isValidPreset('es5-unbundled'), false);
      assert.equal(isValidPreset('es5'), false);
      assert.equal(isValidPreset('es6'), false);
      assert.equal(isValidPreset('js-compile'), false);
      assert.equal(isValidPreset(''), false);
      // tslint:disable:no-any
      assert.equal(isValidPreset(null as any), false);
      assert.equal(isValidPreset(undefined as any), false);
      assert.equal(isValidPreset(0 as any), false);
      assert.equal(isValidPreset(1 as any), false);
      // tslint:enable:no-any
    });
  });

  suite('applyBuildPreset()', () => {
    test('applies es5-bundled preset', () => {
      const givenBuildConfig = {preset: 'es5-bundled'};
      const expectedBuildConfig: ProjectBuildOptions = {
        name: 'es5-bundled',
        preset: 'es5-bundled',
        js: {
          minify: true,
          compile: 'es5',
          transformModulesToAmd: true,
        },
        css: {minify: true},
        html: {minify: true},
        bundle: true,
        addServiceWorker: true,
        addPushManifest: false,
      };
      assert.deepEqual(applyBuildPreset(givenBuildConfig), expectedBuildConfig);
    });

    test('applies es6-bundled preset', () => {
      const givenBuildConfig = {preset: 'es6-bundled'};
      const expectedBuildConfig: ProjectBuildOptions = {
        name: 'es6-bundled',
        preset: 'es6-bundled',
        browserCapabilities: ['es2015'],
        js: {
          minify: true,
          compile: 'es2015',
          transformModulesToAmd: true,
        },
        css: {minify: true},
        html: {minify: true},
        bundle: true,
        addServiceWorker: true,
        addPushManifest: false,
      };
      assert.deepEqual(applyBuildPreset(givenBuildConfig), expectedBuildConfig);
    });

    test('applies es6-unbundled preset', () => {
      const givenBuildConfig = {preset: 'es6-unbundled'};
      const expectedBuildConfig: ProjectBuildOptions = {
        name: 'es6-unbundled',
        preset: 'es6-unbundled',
        browserCapabilities: ['es2015', 'push'],
        js: {
          minify: true,
          compile: 'es2015',
          transformModulesToAmd: true,
        },
        css: {minify: true},
        html: {minify: true},
        bundle: false,
        addServiceWorker: true,
        addPushManifest: true,
      };
      assert.deepEqual(applyBuildPreset(givenBuildConfig), expectedBuildConfig);
    });

    test('applies provided config options as overrides to preset', () => {
      const givenBuildConfig: ProjectBuildOptions = {
        preset: 'es5-bundled',
        name: 'name-override',
        js: {
          minify: false,
          compile: false,
          transformModulesToAmd: false,
        },
        css: {minify: false},
        html: {minify: false},
        bundle: false,
        addServiceWorker: false,
        addPushManifest: false,
      };
      const expectedBuildConfig: ProjectBuildOptions = {
        preset: 'es5-bundled',
        name: 'name-override',
        js: {
          minify: false,
          compile: false,
          transformModulesToAmd: false,
        },
        css: {minify: false},
        html: {minify: false},
        bundle: false,
        addServiceWorker: false,
        addPushManifest: false,
      };
      assert.deepEqual(applyBuildPreset(givenBuildConfig), expectedBuildConfig);
    });

    test('returns the same config if no preset is provided', () => {
      const givenBuildConfig: ProjectBuildOptions = {
        name: 'no-preset',
        js: {minify: true, compile: false},
        css: {minify: true},
        html: {minify: true},
      };
      assert.deepEqual(applyBuildPreset(givenBuildConfig), givenBuildConfig);
    });

    test('returns the same config if preset is provided but not found', () => {
      const givenBuildConfig: ProjectBuildOptions = {
        preset: 'not-a-real-preset-name',
        js: {minify: false, compile: true},
        css: {minify: true},
        html: {minify: true},
      };
      assert.deepEqual(applyBuildPreset(givenBuildConfig), givenBuildConfig);
    });
  });
});
