import {html, LitElement, property} from 'lit-element';
import {ConnectionState} from "./ConnectionState";

const DEFAULT_THEME = `
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

export class LoadingIndicator extends LitElement {

  @property({type: Number })
  firstDelay: number = 300;

  @property({type: Number })
  secondDelay: number = 1500;

  @property({type: Number })
  thirdDelay: number = 5000;

  @property({type: Boolean })
  applyDefaultTheme: boolean = true;

  @property({type: String, attribute: false})
  loadingBarState: LoadingBarState = LoadingBarState.IDLE;

  updateTheme() {
    if (this.applyDefaultTheme) {
      if (!document.getElementById("css-loading-indicator")) {
        const style = document.createElement('style');
        style.setAttribute('type', 'text/css');
        style.setAttribute('id', 'css-loading-indicator');
        style.textContent = DEFAULT_THEME;
        document.head.appendChild(style);
      }
    } else {
      const style = document.getElementById("css-loading-indicator");
      if (style) {
        document.head.removeChild(style);
      }
    }
  }

  connectedCallback() {
    super.connectedCallback();

    this.updateTheme();

    const $wnd = window as any;
    if ($wnd.Vaadin?.Flow?.connectionState) {
      let timeout1st: any;
      let timeout2nd: any;
      let timeout3rd: any;
      $wnd.Vaadin.Flow.connectionState.addStateChangeListener((_: ConnectionState, current: ConnectionState) => {
        clearTimeout(timeout1st);
        clearTimeout(timeout2nd);
        clearTimeout(timeout3rd);
        if (current === ConnectionState.LOADING) {
          this.loadingBarState = LoadingBarState.IDLE;
          timeout1st = setTimeout(() => this.loadingBarState = LoadingBarState.FIRST, this.firstDelay);
          timeout2nd = setTimeout(() => this.loadingBarState = LoadingBarState.SECOND, this.secondDelay);
          timeout3rd = setTimeout(() => this.loadingBarState = LoadingBarState.THIRD, this.thirdDelay);
        } else {
          this.loadingBarState = LoadingBarState.IDLE;
        }
      });
    }
  }

  // render in light DOM
  createRenderRoot() {
    return this;
  }

  getStyleForState(): string {
    switch (this.loadingBarState) {
      case LoadingBarState.IDLE:
        return 'display: none';
      case LoadingBarState.FIRST:
      case LoadingBarState.SECOND:
      case LoadingBarState.THIRD:
        return 'display: block';
    }
  }

  getClassForState(): string {
    switch (this.loadingBarState) {
      case LoadingBarState.IDLE:
        return '';
      case LoadingBarState.FIRST:
        return 'first';
      case LoadingBarState.SECOND:
        return 'second';
      case LoadingBarState.THIRD:
        return 'third';
    }
  }

  render() {
    // this may not be the right place to do this
    this.updateTheme();

    return html`
    <div class="v-loading-indicator ${this.getClassForState()}" style="${this.getStyleForState()}"></div>
  `;
  }
}

enum LoadingBarState {
  IDLE = 'idle',
  FIRST = 'first',
  SECOND ='second',
  THIRD = 'third'
}

if (customElements.get('vaadin-loading-indicator') === undefined) {
  customElements.define('vaadin-loading-indicator', LoadingIndicator);
}
