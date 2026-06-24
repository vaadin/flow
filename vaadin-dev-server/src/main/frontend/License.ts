import { ServerMessage } from './vaadin-dev-tools';
import {
  preTrialStartFailed,
  showPreTrialSplashScreen,
  updateLicenseDownloadStatus
} from './pre-trial-splash-screen';

const noLicenseFallbackTimeout = 1000;

export interface Product {
  name: string;
  version: string;
}

export interface PreTrial {
  trialName?: String;
  trialState: String;
  daysRemaining?: number;
  daysRemainingUntilRenewal?: number;
}

export interface ProductAndMessage {
  message: string;
  messageHtml?: string;
  product: Product;
  preTrial?: PreTrial;
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

  if (element.isConnected) {
    element.outerHTML = `<no-license style="display:flex;align-items:center;text-align:center;justify-content:center;"><div>${htmlMessage}</div></no-license>`;
  }
};

const productTagNames: Record<string, string[]> = {};
const productChecking: Record<string, boolean> = {};
const productMissingLicense: Record<string, ProductAndMessage> = {};
const productCheckOk: Record<string, boolean> = {};

const key = (product: Product): string => {
  return `${product.name}_${product.version}`;
};

const checkLicenseIfNeeded = (cvdlElement: Element) => {
  const { cvdlName, version } = cvdlElement.constructor as CustomElementConstructor & {
    cvdlName: string;
    version: string;
  };
  const product: Product = { name: cvdlName, version };
  const tagName = cvdlElement.tagName.toLowerCase();
  productTagNames[cvdlName] = productTagNames[cvdlName] ?? [];
  productTagNames[cvdlName].push(tagName);

  const failedLicenseCheck = productMissingLicense[key(product)];
  if (failedLicenseCheck) {
    // Has been checked and the check failed
    setTimeout(() => showNoLicenseFallback(cvdlElement, failedLicenseCheck), noLicenseFallbackTimeout);
  }

  if (productMissingLicense[key(product)] || productCheckOk[key(product)]) {
    // Already checked
  } else if (!productChecking[key(product)]) {
    // Has not been checked
    productChecking[key(product)] = true;
    (window as any).Vaadin.devTools.checkLicense(product);
  }
};

export const licenseCheckOk = (data: Product) => {
  productCheckOk[key(data)] = true;

  // eslint-disable-next-line no-console
  console.debug('License check ok for', data);
};

export const licenseCheckFailed = (data: ProductAndMessage) => {
  const productName = data.product.name;
  productMissingLicense[key(data.product)] = data;
  // eslint-disable-next-line no-console
  console.error('License check failed for', productName);

  const tags = productTagNames[productName];
  if (tags?.length > 0) {
    findAll(document, tags).forEach((element) => {
      setTimeout(
        () => showNoLicenseFallback(element, productMissingLicense[key(data.product)]),
        noLicenseFallbackTimeout
      );
    });
  }
};

export const licenseCheckNoKey = (data: ProductAndMessage) => {
  const keyUrl = data.message;

  const productName = data.product.name;
  data.messageHtml = `No license found. <a target=_blank onclick="javascript:window.open(this.href);return false;" href="${keyUrl}">Go here to start a trial or retrieve your license.</a>`;
  productMissingLicense[key(data.product)] = data;
  // eslint-disable-next-line no-console
  console.error('No license found when checking', productName);

  const tags = productTagNames[productName];
  if (tags?.length > 0) {
    findAll(document, tags).forEach((element) => {
      setTimeout(
        () => showNoLicenseFallback(element, productMissingLicense[key(data.product)]),
        noLicenseFallbackTimeout
      );
    });
  }
};

export const handleLicenseMessage = (message: ServerMessage, bodyShadowRoot: ShadowRoot | null): boolean => {
  if (message.command === 'license-check-ok') {
    licenseCheckOk(message.data);
    return true;
  } else if (message.command === 'license-check-failed') {
    licenseCheckFailed(message.data);
    return true;
  } else if (message.command === 'license-check-nokey') {
    showPreTrialSplashScreen(bodyShadowRoot, message.data);
    licenseCheckNoKey(message.data);
    return true;
  } else if (message.command === 'license-pretrial-started') {
    console.debug('Pre-trial period started', message.data);
    window.location.reload();
    return true;
  } else if (message.command === 'license-pretrial-expired') {
    console.debug('Pre-trial period expired', message.data);
    preTrialStartFailed(true, bodyShadowRoot);
    return true;
  } else if (message.command === 'license-pretrial-failed') {
    console.debug('Pre-trial period start failed', message.data);
    preTrialStartFailed(false, bodyShadowRoot);
    return true;
  } else if (message.command === 'license-download-completed') {
    console.debug('License downloaded');
    window.location.reload();
    return true;
  } else if (message.command === 'license-download-started') {
    updateLicenseDownloadStatus('started', bodyShadowRoot);
    return true;
  } else if (message.command === 'license-download-failed') {
    updateLicenseDownloadStatus('failed', bodyShadowRoot);
    return true;
  }
  return false;
};

export const startPreTrial = () => {
  (window as any).Vaadin.devTools.startPreTrial();
};
export const tryAcquireLicense = () => {
  const products = Object.values(productMissingLicense);
  if (products.length > 0) {
    (window as any).Vaadin.devTools.downloadLicense(products[0].product);
  }
};

export const licenseInit = () => {
  // Process already registered elements
  (window as any).Vaadin.devTools.createdCvdlElements.forEach((element: Element) => {
    checkLicenseIfNeeded(element);
  });

  // Handle new elements directly
  (window as any).Vaadin.devTools.createdCvdlElements = {
    push: (element: Element) => {
      checkLicenseIfNeeded(element);
    }
  };
};
