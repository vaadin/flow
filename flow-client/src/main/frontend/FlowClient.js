// init() is the client entry point that Flow.ts imports and calls. It runs the
// TypeScript bootstrap (onModuleLoad): it registers the widgetset start callback
// so the server bootstrap can start the application, which assembles and starts
// the TypeScript engine (ApplicationConnection).
// The optional atmosphere argument is the atmosphere.js module that Flow.ts
// already loaded, for the push connection to use instead of loading it again.
import { onModuleLoad } from './internal/client/bootstrap/Bootstrapper';
import { setAtmosphereModule } from './internal/client/communication/AtmosphereLoader';

export function init(atmosphere) {
  if (atmosphere) {
    setAtmosphereModule(atmosphere);
  }
  onModuleLoad();
}
