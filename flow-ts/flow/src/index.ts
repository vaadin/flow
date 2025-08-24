import typescriptPart from './vaadin-object/base.ts';
import consoleErrorsPart from './vaadin-object/console-errors.ts';
import devToolsPluginPart from './vaadin-object/dev-tools-plugin.ts';
import devToolsPart from './vaadin-object/dev-tools.ts';
import developmentModePart from './vaadin-object/development-mode.ts';

window.Vaadin = {
  ...consoleErrorsPart,
  ...developmentModePart,
  ...devToolsPart,
  ...devToolsPluginPart,
  ...typescriptPart
};

// @ts-expect-error: esbuild injection
// eslint-disable-next-line @typescript-eslint/no-unsafe-call
__REGISTER__();
