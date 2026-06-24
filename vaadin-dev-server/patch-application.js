// Patches a Flow application's frontend to use the local vaadin-dev-tools, in
// order to be able to work on the dev tools UI within an actual application
// Usage: npm run patch-app /path/to/flow-app
const fs = require('fs');
const path = require('path');

// Verify that the script was passed a path to a Flow application
const appPath = process.argv[2];
if (!appPath) {
  throw new Error(
    'Missing application path argument. The patch-app script should be run as: npm run patch-app /path/to/flow-app'
  );
}

let vaadinFilePath = path.join(appPath, 'src', 'main', 'frontend', 'generated', 'vaadin.ts');
if (!fs.existsSync(vaadinFilePath)) {
  vaadinFilePath = path.join(appPath, 'frontend', 'generated', 'vaadin.ts');
}
if (!fs.existsSync(vaadinFilePath)) {
  throw new Error(
    `Application path does not contain a ./src/main/frontend/generated directory: ${vaadinFilePath}. Make sure to start the app first.`
  );
}

// Patch content of vaadin.ts to:
// - exclude vaadin-dev-tools from classpath
// - add HMR script from @web/dev-server
// - add the vaadin-dev-tools hosted by web-dev-server with HMR support
const webDevServerUrl = 'http://localhost:8000';
const devToolsImport = "import 'Frontend/generated/jar-resources/vaadin-dev-tools/vaadin-dev-tools.js';";
const vaadinFileContent = fs.readFileSync(vaadinFilePath, 'utf8');

const isPatched = vaadinFileContent.includes('__web-dev-server__web-socket.js');
const hasDevToolsImport = vaadinFileContent.includes(devToolsImport);

if (isPatched) {
  console.log(`✅ ${vaadinFilePath} is already patched`);
} else if (hasDevToolsImport) {
  const patchContent = `
const hmrScript = document.createElement("script");
hmrScript.setAttribute("type", "module");
hmrScript.setAttribute("src", "${webDevServerUrl}/__web-dev-server__web-socket.js");
document.body.append(hmrScript);

const devToolsScript = document.createElement("script");
devToolsScript.setAttribute("type", "module");
devToolsScript.setAttribute("src", "${webDevServerUrl}/src/main/frontend/vaadin-dev-tools.ts");
document.body.append(devToolsScript);
`;

  const patchedVaadinFileContent = vaadinFileContent.replace(devToolsImport, patchContent);
  fs.writeFileSync(vaadinFilePath, patchedVaadinFileContent, 'utf8');
  console.log(`✅ Patched ${vaadinFilePath}`);
} else {
  console.log(`❌ Failed to patch ${vaadinFilePath}. The file does not seem to contain an import for the dev tools.`);
}
