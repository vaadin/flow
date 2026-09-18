"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved. This
 * code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be
 * found at http://polymer.github.io/AUTHORS.txt The complete set of
 * contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt Code
 * distributed by Google as part of the polymer project is also subject to an
 * additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
require("source-map-support/register");
const fsExtra = require("fs-extra");
const glob = require("glob");
const path = require("path");
const gen_ts_1 = require("./gen-ts");
const verify_1 = require("./verify");
const commandLineArgs = require("command-line-args");
const commandLineUsage = require("command-line-usage");
const argDefs = [
    {
        name: 'help',
        type: Boolean,
        description: 'Print this help text.',
    },
    {
        name: 'version',
        type: Boolean,
        description: 'Print the installed version.',
    },
    {
        name: 'root',
        type: String,
        defaultValue: '.',
        description: 'Root directory of the package to analyze (default ".").',
    },
    {
        name: 'config',
        type: String,
        description: 'JSON configuration file (default "<root>/gen-tsd.json" if exists).',
    },
    {
        name: 'outDir',
        type: String,
        description: 'Type declarations output directory (required).',
    },
    {
        name: 'deleteExisting',
        type: Boolean,
        description: 'Recursively delete all .d.ts files in <outDir> before ' +
            'writing new typings, excluding node_modules/, bower_components/, ' +
            'or any file added using the <addReferences> or <autoImport> config ' +
            'options.',
    },
    {
        name: 'verify',
        type: Boolean,
        description: 'Compile the generated types with TypeScript 3.0 and fail ' +
            'if they are invalid.',
    },
    {
        name: 'googModules',
        type: Boolean,
        description: 'If true, outputs declarations in \'goog:\' modules instead' +
            ' of using simple ES modules. This is a temporary hack to account for' +
            ' how modules are resolved for TypeScript inside google3. This' +
            ' is probably not at all useful for anyone but the Polymer team.'
    }
];
function run(argv) {
    return __awaiter(this, void 0, void 0, function* () {
        const args = commandLineArgs(argDefs, { argv });
        if (args.help) {
            console.log(commandLineUsage([
                {
                    header: `gen-typescript-declarations`,
                    content: 'https://github.com/Polymer/tools/tree/master/packages/' +
                        'gen-typescript-declarations',
                },
                {
                    header: `Options`,
                    optionList: argDefs,
                }
            ]));
            return;
        }
        if (args.version) {
            console.log(require('../package.json').version);
            return;
        }
        if (!args.outDir) {
            throw new Error('--outDir is required');
        }
        const outDir = path.resolve(args.outDir);
        if (!args.config) {
            const p = path.join(args.root, 'gen-tsd.json');
            if (yield fsExtra.pathExists(p)) {
                args.config = p;
            }
        }
        let config = {};
        if (args.config) {
            console.info(`Loading config from "${args.config}".`);
            config = JSON.parse(yield fsExtra.readFile(args.config, 'utf8'));
        }
        if (args.googModules) {
            config.googModules = true;
        }
        const fileMap = yield gen_ts_1.generateDeclarations(args.root, config);
        if (args.deleteExisting) {
            let dtsFiles = glob.sync('**/*.d.ts', {
                cwd: outDir,
                absolute: true,
                nodir: true,
                ignore: [
                    'node_modules/**',
                    'bower_components/**',
                ]
            });
            // If the addReferences or autoImport options are used, it's probably to add
            // some manually written typings. Since manually written typing files won't
            // get re-generated, we shouldn't delete them.
            const dontDelete = new Set();
            for (const dtsFilePaths of Object.values(config.addReferences || {})) {
                for (const dtsFilePath of dtsFilePaths) {
                    dontDelete.add(path.resolve(args.root, dtsFilePath));
                }
            }
            for (const localModuleSpecifier of Object.keys(config.autoImport || {})) {
                const dtsFilePath = localModuleSpecifier.replace(/\.js$/, '') + '.d.ts';
                dontDelete.add(path.resolve(args.root, dtsFilePath));
            }
            dtsFiles = dtsFiles.filter((filepath) => !dontDelete.has(filepath));
            console.log(`Deleting ${dtsFiles.length} existing d.ts files from ${outDir}`);
            yield Promise.all(dtsFiles.map((filepath) => fsExtra.remove(filepath)));
        }
        console.log(`Writing type declarations to ${outDir}`);
        yield writeFileMap(args.outDir, fileMap);
        if (args.verify) {
            console.log('Verifying type declarations');
            const declarationFiles = [...fileMap.keys()].map((filePath) => path.join(outDir, filePath));
            const result = verify_1.verifyTypings(declarationFiles);
            if (result.success === true) {
                console.log('Compilation successful');
            }
            else {
                console.log('Compilation failed');
                console.log(result.errorLog);
                process.exit(1);
            }
        }
    });
}
function writeFileMap(rootDir, files) {
    return __awaiter(this, void 0, void 0, function* () {
        const promises = [];
        for (const [relPath, contents] of files) {
            const fullPath = path.join(rootDir, relPath);
            promises.push(fsExtra.outputFile(fullPath, contents));
        }
        yield Promise.all(promises);
    });
}
(() => __awaiter(this, void 0, void 0, function* () {
    try {
        yield run(process.argv);
    }
    catch (e) {
        console.error(e);
        process.exit(1);
    }
}))();
//# sourceMappingURL=cli.js.map