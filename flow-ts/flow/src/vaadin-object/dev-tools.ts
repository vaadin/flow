import type { MaybePromise } from '../types.js';

declare global {
  interface VaadinLicenseChecker {
    maybeCheck(productInfo: unknown): void;
  }

  interface VaadinDevTools {
    createdCvdlElements: readonly HTMLElement[];
  }

  interface Vaadin {
    VaadinLicenseChecker: VaadinLicenseChecker;
    devTools: VaadinDevTools;
    originalCustomElementDefineFn: typeof customElements.define;
  }
}

export interface CustomElement {
  connectedCallback?(): MaybePromise<void>;
  disconnectedCallback?(): MaybePromise<void>;
  adoptedCallback?(): MaybePromise<void>;
  attributeChangedCallback?(name: string, oldValue: string | null, newValue: string | null): MaybePromise<void>;
}

export interface CustomElementConstructorWithPrototype extends CustomElementConstructor {
  prototype: CustomElement;
}

const originalCustomElementDefineFn = customElements.define;

const createdCvdlElements: HTMLElement[] = [];

Object.defineProperty(customElements, 'define', {
  configurable: true,
  value(name: string, constructor: CustomElementConstructorWithPrototype, options?: ElementDefinitionOptions) {
    if ('cvdlName' in constructor && constructor.cvdlName && 'version' in constructor && constructor.version) {
      const { connectedCallback } = constructor.prototype;

      Object.defineProperty(constructor.prototype, 'connectedCallback', {
        configurable: true,
        async value(this: HTMLElement) {
          createdCvdlElements.push(this);

          if (connectedCallback) {
            await connectedCallback.call(this);
          }
        }
      });
    }

    originalCustomElementDefineFn.call(customElements, name, constructor, options);
  }
});

const part: Pick<Vaadin, 'VaadinLicenseChecker' | 'devTools' | 'originalCustomElementDefineFn'> = {
  VaadinLicenseChecker: {
    maybeCheck(_productInfo: unknown) {}
  },
  devTools: {
    createdCvdlElements
  },
  originalCustomElementDefineFn
};

export default part;
