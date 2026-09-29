import { Checker } from '../../Checker.js';
import 'vite';
import '../../worker.js';
import '../../types.js';
import 'node:worker_threads';
import 'eslint';
import 'stylelint';
import '../vls/initParams.js';
import 'vscode-uri';
import 'vscode-languageserver/node';

declare let createServeAndBuild: any;
declare class StylelintChecker extends Checker<'stylelint'> {
    constructor();
    init(): void;
}

export { StylelintChecker, createServeAndBuild };
