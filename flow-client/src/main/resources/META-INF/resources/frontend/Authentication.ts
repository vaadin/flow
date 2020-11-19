/* tslint:disable:max-classes-per-file */

import { MiddlewareClass, MiddlewareContext, MiddlewareNext } from "./Connect";

export interface LoginResult {
  error: boolean;
  token?: string;
  errorTitle?: string;
  errorMessage?: string;
}

export interface LoginOptions{
  loginProcessingUrl?: string;
  failureUrl?: string;
  defaultSuccessUrl?: string;
}

export interface LogoutOptions{
  logoutUrl?: string;
}

/**
 * A helper method for Spring Security based form login.
 * @param username 
 * @param password 
 * @param options defines additional options, e.g, the loginProcessingUrl, failureUrl, defaultSuccessUrl etc.
 */
export async function login(username: string, password: string, options?: LoginOptions): Promise<LoginResult> {
  let result;
  try {
    const data = new FormData();
    data.append('username', username);
    data.append('password', password);

    const loginProcessingUrl = options && options.loginProcessingUrl ? options.loginProcessingUrl : '/login'; 
    const response = await fetch(loginProcessingUrl, {method: 'POST', body: data});

    const failureUrl = options && options.failureUrl ? options.failureUrl : '/login?error'; 
    const defaultSuccessUrl = options && options.defaultSuccessUrl ? options.defaultSuccessUrl : '/'
    // this assumes the default Spring Security form login configuration (handler URL and responses)
    if (response.ok && response.redirected && response.url.endsWith(failureUrl)) {
      result = {
        error: true,
        errorTitle: 'Incorrect username or password.',
        errorMessage: 'Check that you have entered the correct username and password and try again.'
      };
    } else if (response.ok && response.redirected && response.url.endsWith(defaultSuccessUrl)) {
      // TODO: find a more efficient way to get a new CSRF token
      // parsing the full response body just to get a token may be wasteful
      const token = getCsrfTokenFromResponseBody(await response.text());
      if (token) {
        (window as any).Vaadin.TypeScript = (window as any).Vaadin.TypeScript || {};
        (window as any).Vaadin.TypeScript.csrfToken = token;
        result = {
          error: false,
          errorTitle: '',
          errorMessage: '',
          token
        };
      }
    }
  } catch (e) {
    result = {
      error: true,
      errorTitle: e.name,
      errorMessage: e.message
    }
  }

  return result || {
    error: true,
    errorTitle: 'Error',
    errorMessage: 'Something went wrong when trying to login.',
  };
}

/**
 * A helper method for Spring Security based form logout
 * @param options defines additional options, e.g, the logoutUrl.
 */
export async function logout(options?: LogoutOptions) {
  // this assumes the default Spring Security logout configuration (handler URL)
  const logoutUrl = options && options.logoutUrl ? options.logoutUrl : '/logout';
  const response = await fetch(logoutUrl);

  // TODO: find a more efficient way to get a new CSRF token
  // parsing the full response body just to get a token may be wasteful
  const token = getCsrfTokenFromResponseBody(await response.text());
  (window as any).Vaadin.TypeScript.csrfToken = token;
}

const getCsrfTokenFromResponseBody = (body: string): string | undefined => {
  const match = body.match(/window\.Vaadin = \{TypeScript: \{"csrfToken":"([0-9a-zA-Z\-]{36})"}};/i);
  return match ? match[1] : undefined;
}

/**
 * It defines what to do when it detects a session is invalid. E.g., 
 * show a login view.
 * It takes an <code>EndpointCallContinue</code> parameter, which can be 
 * used to continue the endpoint call.  
 */
export type OnInvalidSessionCallback = () => Promise<LoginResult>;

/**
 * A helper class for handling invalid sessions during an endpoint call.
 * E.g., you can use this to show user a login page when the session has expired.
 */
export class InvalidSessionMiddleware implements MiddlewareClass {
  constructor(private onInvalidSessionCallback: OnInvalidSessionCallback) {}

  async invoke(context: MiddlewareContext, next: MiddlewareNext): Promise<Response> {
    const clonedContext = { ...context };
    clonedContext.request = context.request.clone();
    const response = await next(context);
    if (response.status === 401) {
      const loginResult = await this.onInvalidSessionCallback();
      if(loginResult.token){
        clonedContext.request.headers.set('X-CSRF-Token', loginResult.token);
        return next(clonedContext);
      }
    } 
    return response;
  }
}