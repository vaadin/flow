// Patches a Flow application's frontend to use the local vaadin-dev-tools, in
// order to be able to work on the dev tools UI within an actual application
// Usage: npm run patch-app /path/to/flow-app
const fs = require('fs');
const path = require('path');

// Verify that the script was passed a path to a Flow application
const appPath = process.argv[2];

if (!appPath) {
  throw new Error("Missing application path argument. " +
    "The patch-app script should be run as: npm run patch-app /path/to/flow-app");
}

const generatedFrontendPath = path.join(appPath, 'frontend', 'generated');

if (!fs.existsSync(generatedFrontendPath)) {
  throw new Error(`Application path does not contain a ./frontend/generated directory: ${generatedFrontendPath}. Make sure to start the app first.`);
}

// Patch content of vaadin.ts to:
// - exclude vaadin-dev-tools from classpath
// - add HMR script from @web/dev-server
// - add the vaadin-dev-tools hosted by web-dev-server with HMR support
const webDevServerUrl = 'http://localhost:8000';
const vaadinFilePath = path.join(generatedFrontendPath, 'vaadin.ts');
const vaadinFileContent = `
import './vaadin-featureflags.ts';
import './index';
import { applyTheme } from './theme.js';

applyTheme(document);

const hmrScript = document.createElement("script");
hmrScript.setAttribute("type", "module");
hmrScript.setAttribute("src", "${webDevServerUrl}/__web-dev-server__web-socket.js");
document.body.append(hmrScript);

const devToolsScript = document.createElement("script");
devToolsScript.setAttribute("type", "module");
devToolsScript.setAttribute("src", "${webDevServerUrl}/src/main/resources/META-INF/frontend/vaadin-dev-tools/vaadin-dev-tools.ts");
document.body.append(devToolsScript);
`;

fs.writeFileSync(vaadinFilePath, vaadinFileContent, 'utf8');

console.log(`✅ Patched ${vaadinFilePath}`);
