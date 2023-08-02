/**
 * Resolves HTMLElement that should be considered instead of directly picked element.
 *
 * Used with overlays that have different HTMLElements visible than present in node tree.
 *
 * MatcherResolvers cannot be added to component metadata as they are dynamically imported after being picked.
 */

const _cookieConsentMatcherResolver = <MatcherResolver>{
  matches: (element: HTMLElement) => {
    return element.classList.contains('cc-banner');
  },
  resolves: (_element: HTMLElement) => <HTMLElement>document.querySelector('vaadin-cookie-consent')
};

const _loginFormOverlayMatcherResolver = <MatcherResolver>{
  matches: (element: HTMLElement) => {
    return element.localName === 'vaadin-login-overlay-wrapper';
  },
  resolves: (_element: HTMLElement) => <HTMLElement>document.querySelector('vaadin-login-overlay')
};

const _matcherResolvers = <MatcherResolver[]>[_cookieConsentMatcherResolver, _loginFormOverlayMatcherResolver];

type MatcherResolver = {
  matches: (element: HTMLElement) => boolean;
  resolves: (element: HTMLElement) => HTMLElement;
};

class ComponentResolver {
  resolveElement(element: HTMLElement) {
    const resolved = _matcherResolvers
      .filter((mr) => this._isMatchingRecursive(mr.matches, element))
      .map((mr) => mr.resolves(element))
      .pop();
    return resolved ?? element;
  }

  _isMatchingRecursive(matcher: (element: HTMLElement) => boolean, element: HTMLElement): HTMLElement | undefined {
    if (matcher(element)) {
      return element;
    } else if (element.parentNode && element.parentNode instanceof HTMLElement) {
      return this._isMatchingRecursive(matcher, element.parentNode);
    } else {
      return undefined;
    }
  }
}

export const componentResolver = new ComponentResolver();
