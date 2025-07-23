import { ProductAndMessage } from './License';

class PreTrial extends HTMLElement {
  #parentObserver: MutationObserver | null;
  #shadowRoot: ShadowRoot;
  #trialExpired: boolean;
  #startFailed: boolean | null;
  private remove: Function;

  constructor() {
    super();

    this.#parentObserver = null;
    this.#trialExpired = false;
    this.#startFailed = null;

    // Create a shadow DOM for encapsulation
    this.#shadowRoot = this.attachShadow({ mode: 'closed' });

    // Initialize the component
    this.render();
    this.setupProtection();
  }

  // Define the observed attributes for the web component
  static get observedAttributes(): string[] {
    return ['expired', 'start-failure'];
  }

  private render(): void {
    // Use template string for HTML structure
    const commonStyles = `
      <style>
        :host {
          position: fixed;
          bottom: 0;
          left: 0;
          right: 0;
          z-index: 9999;
          min-height: 100% !important;
          min-width: 100% !important;
          display: flex !important;
          visibility: visible !important;
          opacity: 1 !important;
          clip-path: none !important;
          text-indent: 0 !important;          
          background-color: rgba(0, 0, 0, 0.5);          
        }
        
        .container {
          margin: auto;
          padding: 20px;
          max-width: 600px;
          border: 1px solid #ccc;
          border-radius: 8px;
          background-color: white;
          opacity: 1 !important;
        }
        
        h3 {
          margin: 0 0 10px 0;
          font-weight: bold;
          color: black;
        }
        
        p {
          margin: 0 0 10px 0;
        }
        
        a {
          color: #007bff;
          text-decoration: none;
        }
        
        .button-container {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        
        button {                    
          padding: 8px 16px;
          border-radius: 4px;
          cursor: pointer;
          font-family: inherit;
          font-size-adjust: none;
          border: none;
        }
        
        .action-button {
          background-color: #007bff;
          color: white;
        }
        
        .login-button {
          background-color: #ededed;
        }
        
        svg {
          height: 1em; /* Match the current font size */
          width: auto; /* Maintain aspect ratio */
          vertical-align: text-top;
        }
        .arrow-icon {
          margin-left: 5px;
          font-size: 16px;
        }
        
        strong {
          font-weight: bold;
        }

        div.error {
          color: #9f3a38;
          background-color: #fff6f6;
          box-shadow: 0 0 0 1px #e0b4b4 inset, 0 0 0 0 transparent;
          padding: 1em 1.5em;
          border-radius: .3rem;
          margin: 0 0 10px 0;
        }
        div.error h4 { margin: 0}
      </style>
     `;

    const vaadinLogo = `<svg class='vaadin-logo' width='24' height='24' viewBox='0 0 77.27 73.82' xmlns='http://www.w3.org/2000/svg'>
        <title>vaadin-logo</title>
        <path d='M38.65 73.82a4.55 4.55 0 0 0 4.14-2.67l.11-.19 15.88-29.39a5.09 5.09 0 0 0-4.42-7.63c-2.26 0-3.79 1.11-4.83 3.48l-10.89 20.3-10.88-20.27c-1-2.4-2.58-3.5-4.84-3.5a5.09 5.09 0 0 0-4.42 7.62L34.4 71l.07.13a4.55 4.55 0 0 0 4.18 2.73m.01-43.69a4.06 4.06 0 0 0 4.06-4.06v-1a3.86 3.86 0 0 1 3.86-3.84h20.8a9.88 9.88 0 0 0 9.89-9.88V3.78a3.8 3.8 0 0 0-7.1-1.86 3.78 3.78 0 0 0-.48 1.85v2.41a3.86 3.86 0 0 1-3.9 3.82H47a8.13 8.13 0 0 0-8 6.91 12 12 0 0 0-.2 2h-.17a12 12 0 0 0-.2-2 8.13 8.13 0 0 0-8-6.9H11.47A3.86 3.86 0 0 1 7.61 6.2V3.81A3.78 3.78 0 0 0 7.12 2 3.8 3.8 0 0 0 0 3.82v7.61a9.88 9.88 0 0 0 9.89 9.87h20.8a3.86 3.86 0 0 1 3.86 3.84v1a4.06 4.06 0 0 0 4.06 4.06h.05z' data-name='Layer 1' fill='#000000'/>
     </svg>`;

    this.#shadowRoot.innerHTML = `
    ${commonStyles}
    <div class='container'>
      <h3>${vaadinLogo} ${this.#trialExpired ? 'Trial expired' : 'Start a Trial or Log in'}</h3>
      <p>
        To use features such as Vaadin Copilot and other commercial components, you need an active <strong>subscription</strong> or <strong>trial</strong>.
      </p>
      <p>
         <slot name='products'></slot>
      </p>
      ${this.#startFailed ? `
        <div class='error'>
          <h4>Trial cannot be started</h4>
            ${this.#trialExpired ?
        'The trial period has expired. You can get an extended 30-day trial by logging in.'
        : 'The attempt to start the Trial failed. Please try again later or contact support.'}
        </div>`
      : ''
    }
      ${!this.#startFailed && this.#trialExpired ? '<p>You can get an extended 30-day trial by logging in.</p>' : ''}
      <p>
        By activating the trial, you agree to the <a href='https://vaadin.com/commercial-license-and-service-terms'
          class='terms-link' target='_blank'>terms and conditions</a>.
      </p>
      <p>
        This trial includes all commercial tools and components. Production builds during the trial will be watermarked.
      </p>
      <div class='button-container'>
        <button class='action-button'>${this.#trialExpired ? 'Extend trial 30 days' : 'Try for 7 days'}</button>
        <button class='login-button'>
          Log in / Sign up
          <svg xmlns='http://www.w3.org/2000/svg'  width='16' height='16' viewBox='0 0 16 16'>
            <path fill='#444' d='M14 16v-11l-1 1v9h-12v-12h9l1-1h-11v14z'></path>
            <path fill='#444' d='M16 0h-5l1.8 1.8-6.8 6.8 1.4 1.4 6.8-6.8 1.8 1.8z'></path>
          </svg>                  
        </button>          
      </div>
    </div>
      `;

    const actionButton = this.#shadowRoot.querySelector('button.action-button')!;
    actionButton.addEventListener('click', () => {
      if (this.#trialExpired) {
        this.openNewWindow('https://vaadin.com/pricing');
      } else {
        (window as any).Vaadin.devTools.startPreTrial();
      }
    });
    const loginButton = this.#shadowRoot.querySelector('button.login-button')!;
    loginButton.addEventListener('click', () => {
      this.openNewWindow('https://vaadin.com/my/account');
    });
  }

  private openNewWindow(url: string): void {
    const newWindow = window.open(url, '_blank');
    if (newWindow) {
      newWindow.opener = null;
    }
  }

  connectedCallback(): void {
    this.setupParentRemovalProtection();
  }

  disconnectedCallback(): void {
    this.cleanup();
  }

  attributeChangedCallback(name: string, oldValue: string | null, newValue: string | null): void {
    if (name === 'expired') {
      this.handleExpiredChange(newValue !== null && newValue !== 'false');
    } else if (name === 'start-failure') {
      this.handleStartFailed(newValue === 'expired');
    }
  }

  private handleExpiredChange(value: boolean) {
    if (this.#trialExpired !== value) {
      this.#trialExpired = value;
      this.render();
    }
  }

  private handleStartFailed(expired: boolean) {
    if (this.#startFailed !== expired || this.#trialExpired !== expired) {
      this.#trialExpired = expired;
      this.#startFailed = true;
      this.render();
    }
  }

  private setupProtection(): void {
    const originalRemove = Element.prototype.remove;
    this.remove = function(this: PreTrial): void {
      console.debug('Attempt to remove vaadin-pretrial detected - restoring');
      const currentParent = this.parentNode;
      // Let the removal happen
      originalRemove.apply(this, arguments);
      // Re-add the component
      this.restoreSplashScreen(currentParent as Node);
    };

    // Protect against style changes
    this.protectStyles();
  }

  private setupParentRemovalProtection(): void {
    // Protect against removal from parent
    if (!this.#parentObserver && this.parentNode) {
      this.#parentObserver = new MutationObserver((mutations: MutationRecord[]) => {
        mutations.forEach((mutation: MutationRecord) => {
          if (mutation.type === 'childList') {
            mutation.removedNodes.forEach((node: Node) => {
              if (node === this) {
                console.debug('vaadin-pretrial removal detected - restoring');
                this.restoreSplashScreen(mutation.target);
              }
            });
          }
        });
      });

      this.#parentObserver.observe(this.parentNode, {
        childList: true, subtree: true
      });
    }
  }

  private protectStyles(): void {
    // Override style setters
    Object.defineProperty(this, 'style', {
      get(): object {
        // return an empty object to prevent programmatic changes
        return {};
      },
      set(_value: CSSStyleDeclaration): void {
        // prevent setting regular style object
      }
    });
  }

  private cleanup(): void {
    if (this.#parentObserver) {
      this.#parentObserver.disconnect();
    }
  }

  private restoreSplashScreen(maybeParentNode: Node | null): void {
    // Re-add the component if it's removed
    if (!maybeParentNode) {
      // Splash screen component not yet attached
      return;
    }
    setTimeout(() => {
      console.debug('Re-adding vaadin-pretrial component');
      const products = this.querySelector('[slot="products"]');
      if ((maybeParentNode as ParentNode).contains(this)) {
        (maybeParentNode as Node).removeChild(this);
      }
      const splashScreen = document.createElement('vaadin-pretrial');
      if (this.#trialExpired) {
        splashScreen.setAttribute('expired', 'true');
      }
      if (this.#startFailed) {
        splashScreen.setAttribute('start-failure', this.#trialExpired ? 'expired' : '');
      }
      if (products) {
        splashScreen.appendChild(products.cloneNode(true));
      }
      (maybeParentNode as Element).appendChild(splashScreen);
    }, 0);
  }
}

// Register the custom element
customElements.define('vaadin-pretrial', PreTrial);

export const showPreTrialSplashScreen = (shadowRoot: ShadowRoot | null, message: ProductAndMessage) => {
  if (shadowRoot && !shadowRoot.innerHTML.includes('vaadin-pretrial')) {
    const expiredPreTrial = message.preTrial?.trialState === 'EXPIRED';
    shadowRoot.innerHTML = `<slot></slot><vaadin-pretrial ${expiredPreTrial ? 'expired' : ''}>
      This application is using:
      <div slot='products'>
        This application is using:
        <ul>
          <li>${message.product.name}</li>
        </ul>
      </div>
    </vaadin-pretrial>`;
  }
};
export const preTrialStartFailed = (expired: boolean, shadowRoot: ShadowRoot | null) => {
  if (shadowRoot) {
    const element = shadowRoot.querySelector('vaadin-pretrial');
    element?.setAttribute('start-failure', expired ? 'expired' : '');
  }
};
