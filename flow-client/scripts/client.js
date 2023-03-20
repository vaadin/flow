/*
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
const fs = require('fs');

const fromDir = "target/classes/META-INF/resources/VAADIN/static/client/";
const fromFileRegex = /^client-.*\.cache\.js$/;

const sourceDir = "src/main/frontend/";
const targetDir = "target/classes/META-INF/frontend/";
const toFile  = "FlowClient.js";

const fromFileName = fs.readdirSync(fromDir)
    .find(filename => fromFileRegex.exec(filename));

// Read from target
let clientSource = fs.readFileSync(fromDir + fromFileName, 'utf8');

// Wrap with ES module export
clientSource = `export function init() {
${clientSource}
};`;

// Write to source
fs.writeFileSync(sourceDir + toFile, clientSource, 'utf8');

// Write to target (copy '.d.ts' and '.js' files from sourceDir)
fs.mkdirSync(targetDir, {recursive: true});
fs.readdirSync(sourceDir)
  .filter(s => s.endsWith('.d.ts') || s.endsWith('.js'))
  .forEach(file => fs.copyFileSync(sourceDir + file, targetDir + file));
