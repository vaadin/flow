// init() is the client entry point that Flow.ts imports and calls. It runs the
// TypeScript bootstrap (onModuleLoad): it registers the widgetset start callback
// so the server bootstrap can start the application, which assembles and starts
// the TypeScript engine (ApplicationConnection).
import { onModuleLoad } from './internal/client/bootstrap/Bootstrapper';
// Both Flow.ts and an exported web component load the client through this
// file, so the shortcut helpers are there wherever a shortcut can listen
import './Shortcut';

export function init() {
  onModuleLoad();
}
