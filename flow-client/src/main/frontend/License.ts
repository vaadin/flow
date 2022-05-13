const noLicenseFallbackTimeout = 1000;

export interface Product {
  name: string;
  version: string;
}

export interface ProductAndMessage {
  message: string;
  messageHtml?: string;
  product: Product;
}

export const findAll = (element: Element | ShadowRoot | Document, tags: string[]): Element[] => {
  const lightDom = Array.from(element.querySelectorAll(tags.join(', ')));
  const shadowDom = Array.from(element.querySelectorAll('*'))
    .filter((e) => e.shadowRoot)
    .flatMap((e) => findAll(e.shadowRoot!, tags));
  return [...lightDom, ...shadowDom];
};

let licenseCheckListener = false;

const showNoLicenseFallback = (element: Element, productAndMessage: ProductAndMessage) => {
  if (!licenseCheckListener) {
    // When a license check has succeeded, refresh so that all elements are properly shown again
    window.addEventListener(
      'message',
      (e) => {
        if (e.data === 'validate-license') {
          window.location.reload();
        }
      },
      false
    );
    licenseCheckListener = true;
  }
  const overlay = (element as any)._overlayElement;
  if (overlay) {
    if (overlay.shadowRoot) {
      const defaultSlot = overlay.shadowRoot.querySelector('slot:not([name])');
      if (defaultSlot && defaultSlot.assignedElements().length > 0) {
        showNoLicenseFallback(defaultSlot.assignedElements()[0], productAndMessage);
        return;
      }
    }
    showNoLicenseFallback(overlay, productAndMessage);
    return;
  }

  const htmlMessage = productAndMessage.messageHtml
    ? productAndMessage.messageHtml
    : `${productAndMessage.message} <p>Component: ${productAndMessage.product.name} ${productAndMessage.product.version}</p>`.replace(
        /https:([^ ]*)/g,
        "<a href='https:$1'>https:$1</a>"
      );

  element.outerHTML = `<no-license style="display:flex;align-items:center;text-align:center;justify-content:center;"><div>${htmlMessage}</div></no-license>`;
};

const productTagNames: Record<string, string[]> = {};
const productMissingLicense: Record<string, ProductAndMessage> = {};

/* eslint-disable func-names */
const overrideCustomElementsDefine = () => {
  const { define } = window.customElements;

  window.customElements.define = function (
    tagName,
    constructor: CustomElementConstructor & { cvdlName?: string },
    options
  ) {
    const { cvdlName } = constructor;
    if (cvdlName) {
      productTagNames[cvdlName] = productTagNames[cvdlName] ?? [];
      productTagNames[cvdlName].push(tagName);

      const productInfo = productMissingLicense[cvdlName];
      if (productInfo) {
        const { connectedCallback } = constructor.prototype;
        constructor.prototype.connectedCallback = function () {
          setTimeout(() => showNoLicenseFallback(this, productInfo), noLicenseFallbackTimeout);

          if (connectedCallback) {
            connectedCallback.call(this);
          }
        };
      }
    }

    define.call(this, tagName, constructor, options);
  };
};
/* eslint-enable func-names */

export const licenseCheckOk = (data: Product) => {
  // eslint-disable-next-line no-console
  console.debug('License check ok for ', data);
};

export const licenseCheckFailed = (data: ProductAndMessage) => {
  const productName = data.product.name;
  productMissingLicense[productName] = data;
  // eslint-disable-next-line no-console
  console.error('License check failed for ', productName);

  const tags = productTagNames[productName];
  if (tags?.length > 0) {
    findAll(document, tags).forEach((element) => {
      setTimeout(() => showNoLicenseFallback(element, productMissingLicense[productName]), noLicenseFallbackTimeout);
    });
  }
};

export const licenseCheckNoKey = (data: ProductAndMessage) => {
  const keyUrl = data.message;

  const productName = data.product.name;
  data.messageHtml = `No license found. <a target=_blank onclick="javascript:window.open(this.href);return false;" href="${keyUrl}">Go here to start a trial or retrieve your license.</a>`;
  productMissingLicense[productName] = data;
  // eslint-disable-next-line no-console
  console.error('No license found when checking ', productName);

  const tags = productTagNames[productName];
  if (tags?.length > 0) {
    findAll(document, tags).forEach((element) => {
      setTimeout(() => showNoLicenseFallback(element, productMissingLicense[productName]), noLicenseFallbackTimeout);
    });
  }
};

overrideCustomElementsDefine();
