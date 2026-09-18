/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */

import * as fs from 'fs';
import * as path from 'path';
import * as slam from './index';

import cliArgs = require('command-line-args');
import getCliUsage = require('command-line-usage');

const options = [
  {
    name: 'source',
    alias: 's',
    type: String,
    description: 'File to clean',
    defaultOption: true
  },
  {
    name: 'type',
    alias: 't',
    type: function(t: string) {
      return t.toLowerCase();
    },
    description: 'Type of file to clean. Choices are "html" and "css"'
  },
  {
    name: 'help',
    alias: 'h',
    type: Boolean,
    description: 'Print this message'
  }
];

interface Args {
  source: string;
  type?: 'html'|'css'|'unknown';
  help: boolean;
}

const args = cliArgs(options) as Args;

function getUsage() {
  return getCliUsage([
    {
      header: 'css-slam',
      content: 'Minimal CSS, fast'
    },
    {
      header: 'Options',
      optionList: options
    }
  ]);
}

if (args.help) {
  console.log(getUsage());
  process.exit(0);
}

if (!args.type) {
  console.log('Must pick a type with --type|-t!');
  console.log(getUsage());
  process.exit(1);
}
const source = args.source;

function processSource(text: string) {
  const type = args.type;
  if (type === 'html') {
    return slam.html(text);
  } else if (type === 'css') {
    return slam.css(text);
  } else {
    console.error('Only supported types are "html" and "css"!');
    console.error(getUsage());
    process.exit(1);
  }
}

if (source) {
  const clean = processSource(fs.readFileSync(path.resolve(source), 'utf-8'));
  console.log(clean);
} else if (!process.stdin.isTTY) {
  const chunks: string[] = [];
  process.stdin.setEncoding('utf-8');
  process.stdin.on('readable', function() {
    const chunk = process.stdin.read() as string;
    if (chunk !== null) {
      chunks.push(chunk);
    }
  });
  process.stdin.on('end', function() {
    console.log(processSource(chunks.join('')));
  });
} else {
  console.error('Missing source file!');
  console.error('Supply file with STDIN or --source flag');
  console.error(getUsage());
  process.exit(1);
}
