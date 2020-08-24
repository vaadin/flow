

export interface FlowConfig {
  imports ?: () => void;
}

interface AppConfig {
  productionMode: boolean;
  appId: string;
  uidl: object;
  clientRouting: boolean;
}

interface AppInitResponse {
  appConfig: AppConfig;
  pushScript?: string;
}

interface HTMLRouterContainer extends HTMLElement {
  onBeforeEnter ?: (ctx: NavigationParameters, cmd: PreventAndRedirectCommands) => Promise<any>;
  onBeforeLeave ?: (ctx: NavigationParameters, cmd: PreventCommands) => Promise<any>;
  serverConnected ?: (cancel: boolean, url?: NavigationParameters) => void;
}

interface FlowRoute {
  action : (params: NavigationParameters) => Promise<HTMLRouterContainer>;
  path: string;
}

interface FlowRoot {
  $: any ;
  $server: any;
}

export interface NavigationParameters {
  pathname: string;
  search: string;
}

export interface PreventCommands {
  prevent: () => any;
}

export interface PreventAndRedirectCommands extends PreventCommands {
  redirect: (route: string) => any;
}

// flow uses body for keeping references
const flowRoot: FlowRoot = window.document.body as any;
const $wnd = window as any;

/**
 * Client API for flow UI operations.
 */
export class Flow {
  config: FlowConfig;
  response!: AppInitResponse;
  pathname = '';

  // @ts-ignore
  container : HTMLRouterContainer;

  // flag used to inform Testbench whether a server route is in progress
  private isActive = false;
  private baseRegex = /^\//;

  constructor(config?: FlowConfig) {
    flowRoot.$ = flowRoot.$ || [];
    this.config = config || {};

    // TB checks for the existence of window.Vaadin.Flow in order
    // to consider that TB needs to wait for `initFlow()`.
    $wnd.Vaadin = $wnd.Vaadin || {};
    if (!$wnd.Vaadin.Flow) {
      $wnd.Vaadin.Flow = {
        clients: {
          TypeScript: {
            isActive: () => this.isActive
          }
        }
      };
    }

    // Regular expression used to remove the app-context
    const elm = document.head.querySelector('base');
    this.baseRegex = new RegExp('^' +
        // IE11 does not support document.baseURI
        (document.baseURI || elm && elm.href || '/')
            .replace(/^https?:\/\/[^\/]+/i, ''));

    // Put a flow progress-bar in the dom
    this.addLoadingIndicator();
  }

  /**
   * Return a `route` object for vaadin-router in an one-element array.
   *
   * The `FlowRoute` object `path` property handles any route,
   * and the `action` returns the flow container without updating the content,
   * delaying the actual Flow server call to the `onBeforeEnter` phase.
   *
   * This is a specific API for its use with `vaadin-router`.
   */
  get serverSideRoutes(): [FlowRoute] {
    return [{
      path: '(.*)',
      action: this.action
    }];
  }

  private get action(): (params: NavigationParameters) => Promise<HTMLRouterContainer> {
    // Return a function which is bound to the flow instance, thus we can use
    // the syntax `...serverSideRoutes` in vaadin-router.
    // @ts-ignore
    return async (params: NavigationParameters) => {
      // Store last action pathname so as we can check it in events
      this.pathname = params.pathname;

      await this.flowInit();
      // When an action happens, navigation will be resolved `onBeforeEnter`
      this.container.onBeforeEnter = (ctx, cmd) => this.flowNavigate(ctx, cmd);
      // For covering the 'server -> client' use case
      this.container.onBeforeLeave = (ctx, cmd) => this.flowLeave(ctx, cmd);
      return this.container;
    }
  }

  // Send a remote call to `JavaScriptBootstrapUI` to check
  // whether navigation has to be cancelled.
  private async flowLeave(
      // @ts-ignore
      ctx: NavigationParameters,
      cmd?: PreventCommands): Promise<any> {

    // server -> server
    if (this.pathname === ctx.pathname) {
      return Promise.resolve({});
    }
    // 'server -> client'
    return new Promise(resolve => {
      this.showLoading();
      // The callback to run from server side to cancel navigation
      this.container.serverConnected = (cancel) => {
        resolve(cmd && cancel ? cmd.prevent() : {});
        this.hideLoading();
      }

      // Call server side to check whether we can leave the view
      flowRoot.$server.leaveNavigation(this.getFlowRoute(ctx));
    });
  }

  // Send the remote call to `JavaScriptBootstrapUI` to render the flow
  // route specified by the context
  private async flowNavigate(ctx: NavigationParameters, cmd?: PreventAndRedirectCommands): Promise<HTMLElement> {
    return new Promise(resolve => {
      this.showLoading()
      // The callback to run from server side once the view is ready
      this.container.serverConnected = (cancel, redirectContext?: NavigationParameters) => {
        if (cmd && cancel) {
          resolve(cmd.prevent());
        } else if (cmd && cmd.redirect && redirectContext) {
          resolve(cmd.redirect(redirectContext.pathname));
        } else {
          this.container.style.display = '';
          resolve(this.container);
        }
        this.hideLoading();
      };

      // Call server side to navigate to the given route
      flowRoot.$server
          .connectClient(this.container.localName, this.container.id, this.getFlowRoute(ctx));
    });
  }

  private getFlowRoute(context: NavigationParameters | Location): string {
    return (context.pathname + (context.search || '')).replace(this.baseRegex, '');
  }

  // import flow client modules and initialize UI in server side.
  private async flowInit(serverSideRouting = false): Promise<AppInitResponse> {
    // Do not start flow twice
    if (!this.response) {

      // show flow progress indicator
      this.showLoading();

      // Initialize server side UI
      this.response = await this.flowInitUi(serverSideRouting);

      // Enable or disable server side routing
      this.response.appConfig.clientRouting = !serverSideRouting;

      const {pushScript, appConfig} = this.response;

      if (typeof pushScript === 'string') {
        await this.loadScript(pushScript);
      }
      const {appId} = appConfig;

      // Load bootstrap script with server side parameters
      const bootstrapMod = await import('./FlowBootstrap');
      await bootstrapMod.init(this.response);

      // Load custom modules defined by user
      if (typeof this.config.imports === 'function') {
        this.injectAppIdScript(appId);
        await this.config.imports();
      }

      // Load flow-client module
      const clientMod = await import('./FlowClient');
      await this.flowInitClient(clientMod);

      // When client-side router, create a container for server views
      if (!serverSideRouting) {
        // we use a custom tag for the flow app container
        const tag = `flow-container-${appId.toLowerCase()}`;
        this.container = flowRoot.$[appId] = document.createElement(tag);
        this.container.id = appId;

        // It might be that components created from server expect that their content has been rendered.
        // Appending eagerly the container we avoid these kind of errors.
        // Note that the client router will move this container to the outlet if the navigation succeed
        this.container.style.display = 'none';
        document.body.appendChild(this.container);
      }

      // hide flow progress indicator
      this.hideLoading();
    }
    return this.response;
  }

  private async loadScript(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.onload = () => resolve();
      script.onerror = reject;
      script.src = url;
      document.body.appendChild(script);
    });
  }

  private injectAppIdScript(appId: string) {
    const appIdWithoutHashCode = appId.substring(0, appId.lastIndexOf('-'));
    const scriptAppId = document.createElement('script');
    scriptAppId.type = 'module';
    scriptAppId.setAttribute('data-app-id', appIdWithoutHashCode);
    document.body.append(scriptAppId);
  }

  // After the flow-client javascript module has been loaded, this initializes flow UI
  // in the browser.
  private async flowInitClient(clientMod: any): Promise<void> {
    clientMod.init();
    // client init is async, we need to loop until initialized
    return new Promise(resolve => {
      const intervalId = setInterval(() => {
        // client `isActive() == true` while initializing or processing
        const initializing = Object.keys($wnd.Vaadin.Flow.clients)
            .filter(key => key !== 'TypeScript')
            .reduce((prev, id) => prev || $wnd.Vaadin.Flow.clients[id].isActive(), false);
        if (!initializing) {
          clearInterval(intervalId);
          resolve();
        }
      }, 5);
    });
  }

  // Returns the `appConfig` object
  private async flowInitUi(serverSideRouting: boolean): Promise<AppInitResponse> {
    // appConfig was sent in the index.html request
    const initial = $wnd.Vaadin && $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.initial;
    if (initial) {
      $wnd.Vaadin.TypeScript.initial = undefined;
      return Promise.resolve(initial);
    }

    // send a request to the `JavaScriptBootstrapHandler`
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const httpRequest = xhr as any;
      const currentPath = location.pathname || '/';
      const requestPath = `${currentPath}?v-r=init` +
          (serverSideRouting ? `&location=${encodeURI(this.getFlowRoute(location))}` : '');

      httpRequest.open('GET', requestPath);

      httpRequest.onerror = () => reject(new Error(
          `Invalid server response when initializing Flow UI.
        ${httpRequest.status}
        ${httpRequest.responseText}`));

      httpRequest.onload = () => {
        const contentType = httpRequest.getResponseHeader('content-type');
        if (contentType && contentType.indexOf('application/json') !== -1) {
          resolve(JSON.parse(httpRequest.responseText));
        } else {
          httpRequest.onerror();
        }
      };
      httpRequest.send();
    });
  }

  private showLoading() {
    // Make Testbench know that server request is in progress
    this.isActive = true;
    $wnd.Vaadin.Flow.loading(true);
  }

  private hideLoading() {
    // Make Testbench know that server request has finished
    this.isActive = false;
    $wnd.Vaadin.Flow.loading(false);
  }

  // A flow loading indicator
  private addLoadingIndicator() {
    const loading = document.createElement('div');
    loading.classList.add('v-loading-indicator');
    loading.setAttribute('style', 'none');
    document.body.appendChild(loading);

    const style = document.createElement('style');
    style.setAttribute('type', 'text/css');
    style.setAttribute('id', 'css-loading-indicator');
    style.textContent = `
      @keyframes v-progress-start {
        0% {width: 0%;}
        100% {width: 50%;}
      }
      @keyframes v-progress-delay {
        0% {width: 50%;}
        100% {width: 90%;}
      }
      @keyframes v-progress-wait {
        0% {width: 90%; height: 4px;}
        3% {width: 91%;height: 7px;}
        100% {width: 96%;height: 7px;}
      }
      @keyframes v-progress-wait-pulse {
        0% {opacity: 1;}
        50% {opacity: 0.1;}
        100% {opacity: 1;}
      }
      .v-loading-indicator {
        position: fixed !important;
        z-index: 99999;
        left: 0;
        right: auto;
        top: 0;
        width: 50%;
        opacity: 1;
        height: 4px;
        background-color: var(--lumo-primary-color, var(--material-primary-color, blue));
        pointer-events: none;
        transition: none;
        animation: v-progress-start 1000ms 200ms both;
      }
      .v-loading-indicator[style*="none"] {
        display: block !important;
        width: 100% !important;
        opacity: 0;
        animation: none !important;
        transition: opacity 500ms 300ms, width 300ms;
      }
      .v-loading-indicator.second {
        width: 90%;
        animation: v-progress-delay 3.8s forwards;
      }
      .v-loading-indicator.third {
        width: 96%;
        animation: v-progress-wait 5s forwards, v-progress-wait-pulse 1s 4s infinite backwards;
      }
    `;
    document.head.appendChild(style);

    // Share loading methods in flow namespace so as it can
    // be reused in Connect.ts
    let timeout2nd: any;
    let timeout3rd: any;
    $wnd.Vaadin.Flow.loading = (action: boolean) => {
      clearTimeout(timeout2nd);
      clearTimeout(timeout3rd);
      loading.classList.remove('second');
      loading.classList.remove('third');
      if (action) {
        loading.removeAttribute('style');
        timeout2nd = setTimeout(() => loading.classList.add('second'), 1500);
        timeout3rd = setTimeout(() => loading.classList.add('third'), 5000);
      } else {
        loading.setAttribute('style', 'none');
      }
    };
  }
}
