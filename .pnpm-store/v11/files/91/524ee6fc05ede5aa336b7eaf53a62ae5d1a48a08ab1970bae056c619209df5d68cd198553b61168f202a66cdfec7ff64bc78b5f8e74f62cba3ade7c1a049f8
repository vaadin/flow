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

'use strict';
const assert = require('chai').assert;
const path = require('path');
const ProjectConfig = require('..').ProjectConfig;

suite('Project Config', () => {
  suite('ProjectConfig', () => {
    suite('constructor', () => {
      test('sets minimum set of defaults when no options are provided', () => {
        const absoluteRoot = process.cwd();
        const config = new ProjectConfig();
        config.validate();

        assert.deepEqual(config, {
          root: absoluteRoot,
          entrypoint: path.resolve(absoluteRoot, 'index.html'),
          fragments: [],
          allFragments: [path.resolve(absoluteRoot, 'index.html')],
          extraDependencies: [],
          sources: [
            path.resolve(absoluteRoot, 'src/**/*'),
            path.resolve(absoluteRoot, 'index.html'),
          ],
          lint: undefined,
          npm: undefined,
          moduleResolution: 'node',
        });
      });

      test(
          'sets root relative to current working directory when provided',
          () => {
            const relativeRoot = 'public';
            const absoluteRoot = path.resolve(relativeRoot);
            const config = new ProjectConfig({root: relativeRoot});
            config.validate();

            assert.deepEqual(config, {
              root: absoluteRoot,
              entrypoint: path.resolve(absoluteRoot, 'index.html'),
              fragments: [],
              allFragments: [path.resolve(absoluteRoot, 'index.html')],
              extraDependencies: [],
              sources: [
                path.resolve(absoluteRoot, 'src/**/*'),
                path.resolve(absoluteRoot, 'index.html'),
              ],
              lint: undefined,
              npm: undefined,
              moduleResolution: 'node',
            });
          });

      test('sets entrypoint relative to root when provided', () => {
        const relativeRoot = 'public';
        const absoluteRoot = path.resolve(relativeRoot);
        const config =
            new ProjectConfig({root: relativeRoot, entrypoint: 'foo.html'});
        config.validate();

        assert.deepEqual(config, {
          root: absoluteRoot,
          entrypoint: path.resolve(absoluteRoot, 'foo.html'),
          fragments: [],
          allFragments: [path.resolve(absoluteRoot, 'foo.html')],
          extraDependencies: [],
          sources: [
            path.resolve(absoluteRoot, 'src/**/*'),
            path.resolve(absoluteRoot, 'foo.html'),
          ],
          lint: undefined,
          npm: undefined,
          moduleResolution: 'node',
        });
      });

      test('sets shell relative to root when provided', () => {
        const config = new ProjectConfig({shell: 'foo.html'});
        config.validate();

        assert.deepEqual(config, {
          root: process.cwd(),
          entrypoint: path.resolve('index.html'),
          shell: path.resolve('foo.html'),
          fragments: [],
          allFragments: [path.resolve('foo.html')],
          extraDependencies: [],
          sources: [
            path.resolve('src/**/*'),
            path.resolve('index.html'),
            path.resolve('foo.html')
          ],
          lint: undefined,
          npm: undefined,
          moduleResolution: 'node',
        });
      });

      test('sets fragments relative to root when provided', () => {
        const config = new ProjectConfig({fragments: ['foo.html', 'bar.html']});
        config.validate();

        assert.deepEqual(config, {
          root: process.cwd(),
          entrypoint: path.resolve('index.html'),
          fragments: [
            path.resolve('foo.html'),
            path.resolve('bar.html'),
          ],
          allFragments: [
            path.resolve('foo.html'),
            path.resolve('bar.html'),
          ],
          extraDependencies: [],
          sources: [
            path.resolve('src/**/*'),
            path.resolve('index.html'),
            path.resolve('foo.html'),
            path.resolve('bar.html'),
          ],
          lint: undefined,
          npm: undefined,
          moduleResolution: 'node',
        });
      });

      test('adds sources relative to root when provided', () => {
        const relativeRoot = 'public';
        const absoluteRoot = path.resolve(relativeRoot);
        const config = new ProjectConfig(
            {root: relativeRoot, sources: ['src/**/*', 'images/**/*']});
        config.validate();

        assert.deepEqual(config, {
          root: absoluteRoot,
          entrypoint: path.resolve(absoluteRoot, 'index.html'),
          fragments: [],
          allFragments: [path.resolve(absoluteRoot, 'index.html')],
          extraDependencies: [],
          sources: [
            path.resolve(absoluteRoot, 'src/**/*'),
            path.resolve(absoluteRoot, 'images/**/*'),
            path.resolve(absoluteRoot, 'index.html'),
          ],
          lint: undefined,
          npm: undefined,
          moduleResolution: 'node',
        });
      });

      test('sets extraDependencies relative to root when provided', () => {
        const relativeRoot = 'public';
        const absoluteRoot = path.resolve(relativeRoot);
        const config = new ProjectConfig({
          root: relativeRoot,
          extraDependencies: [
            'bower_components/**/*.js',
            '!bower_components/ignore-big-package',
          ],
        });
        config.validate();

        assert.deepEqual(config, {
          root: absoluteRoot,
          entrypoint: path.resolve(absoluteRoot, 'index.html'),
          fragments: [],
          allFragments: [path.resolve(absoluteRoot, 'index.html')],
          extraDependencies: [
            path.resolve(absoluteRoot, 'bower_components/**/*.js'),
            '!' +
                path.resolve(
                    absoluteRoot, 'bower_components/ignore-big-package'),
          ],
          sources: [
            path.resolve(absoluteRoot, 'src/**/*'),
            path.resolve(absoluteRoot, 'index.html'),
          ],
          lint: undefined,
          npm: undefined,
          moduleResolution: 'node',
        });
      });

      test(
          'sets allFragments to fragments & shell when both are provided',
          () => {
            const config = new ProjectConfig({
              fragments: ['foo.html', 'bar.html'],
              shell: 'baz.html',
            });
            config.validate();

            assert.deepEqual(config, {
              root: process.cwd(),
              entrypoint: path.resolve('index.html'),
              shell: path.resolve('baz.html'),
              fragments: [
                path.resolve('foo.html'),
                path.resolve('bar.html'),
              ],
              allFragments: [
                path.resolve('baz.html'),
                path.resolve('foo.html'),
                path.resolve('bar.html'),
              ],
              extraDependencies: [],
              sources: [
                path.resolve('src/**/*'),
                path.resolve('index.html'),
                path.resolve('baz.html'),
                path.resolve('foo.html'),
                path.resolve('bar.html'),
              ],
              lint: undefined,
              npm: undefined,
              moduleResolution: 'node',
            });
          });

      test(
          'builds property is unset when `build` option is not provided',
          () => {
            const config = new ProjectConfig();
            config.validate();

            assert.isUndefined(config.builds);
          });

      test(
          'sets builds property to an array when `build` option is an array',
          () => {
            const config = new ProjectConfig({
              builds: [
                {
                  name: 'bundled',
                  bundle: true,
                  insertPrefetchLinks: true,
                },
                {
                  name: 'unbundled',
                  bundle: false,
                  insertPrefetchLinks: true,
                }
              ]
            });
            config.validate();

            assert.property(config, 'builds');
            assert.deepEqual(config.builds, [
              {
                name: 'bundled',
                bundle: true,
                insertPrefetchLinks: true,
              },
              {
                name: 'unbundled',
                bundle: false,
                insertPrefetchLinks: true,
              }
            ]);
          });

      test('npm option sets other options to expected defaults', () => {
        const config = new ProjectConfig({
          npm: true,
        });
        config.validate();

        assert.equal(config.npm, true);
        assert.equal(config.componentDir, 'node_modules/');
      });

      test('npm option does not override other explicitly set values', () => {
        const config = new ProjectConfig(
            {npm: true, componentDir: '../some_other_dir/over_here/'});
        config.validate();

        assert.equal(config.npm, true);
        assert.equal(config.componentDir, '../some_other_dir/over_here/');
      });

      suite('module resolution', () => {
        test('defaults to node', () => {
          const config = new ProjectConfig({});
          config.validate();
          assert.equal(config.moduleResolution, 'node');
        });

        test('can be set to none', () => {
          const config = new ProjectConfig({moduleResolution: 'none'});
          config.validate();
          assert.equal(config.moduleResolution, 'none');
        });

        test('cannot be set to something invalid', () => {
          const config = new ProjectConfig({moduleResolution: 'magic'});
          assert.throws(() => {
            config.validate();
          });
        });
      });
    });

    suite('isFragment()', () => {
      test('matches all fragments and does not match other file paths', () => {
        const relativeRoot = 'public';
        const absoluteRoot = path.resolve(relativeRoot);
        const config = new ProjectConfig({
          root: relativeRoot,
          entrypoint: 'foo.html',
          fragments: ['bar.html'],
          shell: 'baz.html',
        });
        config.validate();

        assert.isTrue(config.isFragment(config.shell));
        assert.isTrue(
            config.isFragment(path.resolve(absoluteRoot, 'bar.html')));
        assert.isTrue(
            config.isFragment(path.resolve(absoluteRoot, 'baz.html')));
        assert.isFalse(config.isFragment(config.entrypoint));
        assert.isFalse(
            config.isFragment(path.resolve(absoluteRoot, 'foo.html')));
        assert.isFalse(config.isFragment(
            path.resolve(absoluteRoot, 'not-a-fragment.html')));
      });
    });

    suite('isShell()', () => {
      test('matches the shell path and does not match other file paths', () => {
        const relativeRoot = 'public';
        const absoluteRoot = path.resolve(relativeRoot);
        const config = new ProjectConfig({
          root: relativeRoot,
          entrypoint: 'foo.html',
          fragments: ['bar.html'],
          shell: 'baz.html',
        });
        config.validate();

        assert.isFalse(config.isShell(config.entrypoint));
        assert.isTrue(config.isShell(config.shell));
        assert.isFalse(config.isShell(path.resolve(absoluteRoot, 'foo.html')));
        assert.isFalse(config.isShell(path.resolve(absoluteRoot, 'bar.html')));
        assert.isTrue(config.isShell(path.resolve(absoluteRoot, 'baz.html')));
        assert.isFalse(
            config.isShell(path.resolve(absoluteRoot, 'not-a-fragment.html')));
      });
    });

    suite('isSource()', () => {
      test(
          'matches source file paths and does not match other file paths',
          () => {
            const relativeRoot = 'public';
            const absoluteRoot = path.resolve(relativeRoot);
            const config = new ProjectConfig({
              root: relativeRoot,
              entrypoint: 'foo.html',
              fragments: ['bar.html'],
              shell: 'baz.html',
            });
            assert.isTrue(config.isSource(config.entrypoint));
            assert.isTrue(config.isSource(config.shell));
            assert.isTrue(
                config.isSource(path.resolve(absoluteRoot, 'foo.html')));
            assert.isTrue(
                config.isSource(path.resolve(absoluteRoot, 'bar.html')));
            assert.isTrue(
                config.isSource(path.resolve(absoluteRoot, 'baz.html')));
            assert.isFalse(config.isSource(
                path.resolve(absoluteRoot, 'not-a-fragment.html')));
          });
    });

    suite('validate()', () => {
      test('returns true for valid configuration', () => {
        const relativeRoot = 'public';
        const config = new ProjectConfig({
          root: relativeRoot,
          entrypoint: 'foo.html',
          fragments: ['bar.html'],
          shell: 'baz.html',
          sources: ['src/**/*', 'images/**/*'],
          extraDependencies: [
            'bower_components/**/*.js',
          ],
        });

        const validateResult = config.validate();
        assert.isTrue(validateResult);
      });

      test('returns true for negative globs that resolve within root', () => {
        const relativeRoot = 'public';
        const config = new ProjectConfig({
          root: relativeRoot,
          sources: [
            'src/**/*',
            'images/**/*',
            '!images/ignore-some-images',
          ],
          extraDependencies: [
            'bower_components/**/*.js',
            '!bower_components/ignore-big-package',
          ],
        });

        const validateResult = config.validate();
        assert.isTrue(validateResult);
      });

      test('throws an exception when a fragment does not resolve within root', () => {
        const relativeRoot = 'public';
        const config = new ProjectConfig({
          root: relativeRoot,
          fragments: ['../bar.html'],
        });

        assert.throws(
            () => config.validate(),
            /Polymer Config Error: a "fragments" path \(.*bar.html\) does not resolve within root \(.*public\)/);
      });

      test('throws an exception when entrypoint does not resolve within root', () => {
        const relativeRoot = 'public';
        const config = new ProjectConfig({
          root: relativeRoot,
          entrypoint: '../bar.html',
        });

        assert.throws(
            () => config.validate(),
            /Polymer Config Error: entrypoint \(.*bar.html\) does not resolve within root \(.*public\)/);
      });

      test('throws an exception when shell does not resolve within root', () => {
        const relativeRoot = 'public';
        const config = new ProjectConfig({
          root: relativeRoot,
          shell: '/some/absolute/path/bar.html',
        });

        assert.throws(
            () => config.validate(),
            /Polymer Config Error: shell \(.*bar.html\) does not resolve within root \(.*public\)/);
      });

      test('returns true when a single, unnamed build is defined', () => {
        const relativeRoot = 'public';
        const config = new ProjectConfig({
          root: relativeRoot,
          builds: [{
            bundle: true,
            insertPrefetchLinks: true,
          }],
        });

        const validateResult = config.validate();
        assert.isTrue(validateResult);
      });

      test('throws an exception when builds property was not an array', () => {
        const config = new ProjectConfig({
          builds: {
            name: 'bundled',
            bundle: true,
            insertPrefetchLinks: true,
          }
        });
        assert.throws(
            () => config.validate(),
            'Polymer Config Error: "builds" ([object Object]) expected an array of build configurations.');
      });

      test('throws an exception when builds array contains duplicate names', () => {
        const config = new ProjectConfig({
          builds: [
            {
              name: 'bundled',
              bundle: true,
            },
            {
              name: 'bundled',
              bundle: false,
            }
          ]
        });
        assert.throws(
            () => config.validate(),
            'Polymer Config Error: "builds" duplicate build name "bundled" found. Build names must be unique.');
      });

      test('throws an exception when builds array contains an unnamed build', () => {
        const config = new ProjectConfig({
          builds: [
            {
              bundle: true,
            },
            {
              name: 'bundled',
              bundle: false,
            }
          ]
        });
        assert.throws(
            () => config.validate(),
            'Polymer Config Error: all "builds" require a "name" property when there are multiple builds defined.');
      });

      test('throws an exception when builds array contains an invalid preset', () => {
        const config = new ProjectConfig({
          builds: [
            {
              preset: 'not-a-real-preset',
            },
            {
              bundle: true,
            }
          ]
        });
        assert.throws(
            () => config.validate(),
            'Polymer Config Error: "not-a-real-preset" is not a valid  "builds" preset.');
      });
    });
  });

  suite('loadOptionsFromFile()', () => {
    test('throws an exception for polymer.json with invalid syntax', () => {
      const filepath = path.resolve('test/polymer-invalid-syntax.json');
      assert.throws(() => ProjectConfig.loadOptionsFromFile(filepath));
    });

    test('throws an exception for polymer.json with invalid data', () => {
      const filepath = path.resolve('test/polymer-invalid-type.json');
      assert.throws(() => ProjectConfig.loadOptionsFromFile(filepath));
    });

    test('returns null if file is missing', () => {
      const filepath = path.resolve('test/this-file-does-not-exist.json');
      assert.equal(ProjectConfig.loadOptionsFromFile(filepath), null);
    });

    test('reads options from config file', () => {
      const filepath = path.resolve('test/polymer.json');
      const options = ProjectConfig.loadOptionsFromFile(filepath);
      assert.deepEqual(options, {
        root: 'public',
        entrypoint: 'foo.html',
        fragments: ['bar.html'],
        extraDependencies: ['baz.html'],
        sources: ['src/**/*', 'images/**/*'],
      });
    });

    test('reads options from a file with just {} in it', () => {
      const filepath = path.resolve('test/polymer-minimal.json');
      const options = ProjectConfig.loadOptionsFromFile(filepath);
      assert.deepEqual(options, {});
    });
  });

  suite('loadConfigFromFile()', () => {
    test('throws an exception for polymer.json with invalid syntax', () => {
      const filepath = path.resolve('test/polymer-invalid-syntax.json');
      assert.throws(() => ProjectConfig.loadConfigFromFile(filepath));
    });

    test('throws an exception for polymer.json with invalid data', () => {
      const filepath = path.resolve('test/polymer-invalid-type.json');
      assert.throws(() => ProjectConfig.loadConfigFromFile(filepath));
    });

    test('returns null if file is missing', () => {
      const filepath = path.resolve('test/this-file-does-not-exist.json');
      assert.equal(ProjectConfig.loadConfigFromFile(filepath), null);
    });

    test('creates config instance from config file options', () => {
      const config =
          ProjectConfig.loadConfigFromFile(path.resolve('test/polymer.json'));
      config.validate();

      const relativeRoot = 'public';
      const absoluteRoot = path.resolve(relativeRoot);
      assert.deepEqual(config, {
        root: absoluteRoot,
        entrypoint: path.resolve(absoluteRoot, 'foo.html'),
        fragments: [path.resolve(absoluteRoot, 'bar.html')],
        allFragments: [path.resolve(absoluteRoot, 'bar.html')],
        extraDependencies: [path.resolve(absoluteRoot, 'baz.html')],
        sources: [
          path.resolve(absoluteRoot, 'src/**/*'),
          path.resolve(absoluteRoot, 'images/**/*'),
          path.resolve(absoluteRoot, 'foo.html'),
          path.resolve(absoluteRoot, 'bar.html'),
        ],
        lint: undefined,
        npm: undefined,
        moduleResolution: 'node',
      });
    });

    test('reads a valid config from a file with just {} in it', () => {
      const config = ProjectConfig.loadConfigFromFile(
          path.resolve('test/polymer-minimal.json'));
      config.validate();
    });
  });

  suite('json validation', () => {
    test('throws good error messages', () => {
      try {
        ProjectConfig.validateAndCreate({lint: []});
      } catch (e) {
        assert.deepEqual(
            e.message, `Property 'lint' is not of a type(s) object`);
        return;
      }
      throw new Error('Expected validateAndCreate to throw for invalid config');
    });
  });

  suite('toJSON()', () => {
    test('with minimal config', () => {
      const config = ProjectConfig.loadConfigFromFile(
          path.resolve('test/polymer-minimal.json'));
      assert.deepEqual(JSON.parse(config.toJSON()), {
        entrypoint: 'index.html',
        fragments: [],
        sources: [
          'src/**/*',
          'index.html',
        ],
        extraDependencies: [],
        moduleResolution: 'node',
      });
    });

    test('with full config', () => {
      const config = ProjectConfig.loadConfigFromFile(
          path.resolve('test/polymer-full.json'));
      assert.deepEqual(JSON.parse(config.toJSON()), {
        entrypoint: 'entrypoint.html',
        shell: 'shell.html',
        fragments: ['fragment.html'],
        sources: [
          'src/**/*',
          '!images/**/*',
          'entrypoint.html',
          'shell.html',
          'fragment.html',
        ],
        extraDependencies: ['extra.html'],
        moduleResolution: 'node',
        builds: [
          {
            preset: 'es6-bundled',
            name: 'es6-bundled',
            addServiceWorker: true,
            addPushManifest: false,
            bundle: true,
            html: {minify: true},
            css: {minify: true},
            js: {
              minify: true,
              compile: 'es2015',
              transformModulesToAmd: true,
            },
            browserCapabilities: ['es2015']
          },
          {
            name: 'my-build',
            swPrecacheConfig: 'sw.conf',
            browserCapabilities: ['es2015'],
            basePath: true,
            bundle: {treeshake: true},
            html: {
              minify: {
                exclude: [
                  'human-readable-example.html',
                  'weird-ie-comments-issue.html'
                ]
              }
            },
            css: {
              minify: {
                exclude: [
                  'css/human-readable-example.css',
                  'css/advanced-syntax.css'
                ]
              }
            },
            js: {
              minify:
                  {exclude: ['js/unminifiable.js', 'js/already-minified.js']},
              compile: {
                exclude: [
                  'js/breaks-when-compiled.js',
                  'js/no-compilation-necessary.js'
                ]
              },
              transformEsModulesToAmd: true,
            }
          },
        ],
        lint: {
          rules: ['some-rule'],
          warningsToIgnore: ['some-warning'],
          filesToIgnore: ['some .* glob']
        }
      });
    });
  });
});
