/* tslint:disable:max-classes-per-file */

// @ts-ignore
window.Vaadin = window.Vaadin || {};
// @ts-ignore
window.Vaadin.registrations = window.Vaadin.registratios || [];

// @ts-ignore
window.Vaadin.registrations.push({
  is: '@vaadin/flow-frontend/Connect',
  version: '3.0-SNAPSHOT'
});

interface ConnectExceptionData {
  message: string;
  type: string;
  detail?: any;
  validationErrorData?: ValidationErrorData[];
}

const throwConnectException = (errorJson: ConnectExceptionData) => {
  if (errorJson.validationErrorData) {
    throw new VaadinConnectValidationError(
      errorJson.message,
      errorJson.validationErrorData,
      errorJson.type
    );
  } else {
    throw new VaadinConnectError(
      errorJson.message,
      errorJson.type,
      errorJson.detail
    );
  }
};

/**
 * Throws a TypeError if the response is not 200 OK.
 * @param response The response to assert.
 * @ignore
 */
const assertResponseIsOk = async(response: Response): Promise<void> => {
  if (!response.ok) {
    const errorText = await response.text();
    let errorJson: ConnectExceptionData | null;
    try {
      errorJson = JSON.parse(errorText);
    } catch (ignored) {
      // not a json
      errorJson = null;
    }

    if (errorJson !== null) {
      throwConnectException(errorJson);
    } else if (errorText !== null && errorText.length > 0) {
      throw new VaadinConnectError(errorText);
    } else {
      throw new VaadinConnectError(
        'expected "200 OK" response, but got ' +
        `${response.status} ${response.statusText}`
      );
    }
  }
};

/**
 * An exception that gets thrown when the Vaadin Connect backend responds
 * with not ok status.
 */
export class VaadinConnectError extends Error {
  /**
   * The optional name of the exception that was thrown on a backend
   */
  type?: string;

  /**
   * The optional detail object, containing additional information sent
   * from a backend
   */
  detail?: any;

  /**
   * @param message the `message` property value
   * @param type the `type` property value
   * @param detail the `detail` property value
   */
  constructor(message: string, type?: string, detail?: any) {
    super(
      `Message: '${message}', additional details: '${JSON.stringify(detail)}'`);
    this.type = type;
    this.detail = detail;
  }
}

/**
 * An exception that gets thrown if Vaadin Connect backend responds
 * with non-ok status and provides additional info
 * on the validation errors occurred.
 */
export class VaadinConnectValidationError extends VaadinConnectError {
  /**
   * An original validation error message.
   */
  validationErrorMessage: string;
  /**
   * An array of the validation errors.
   */
  validationErrorData: ValidationErrorData[];

  /**
   * @param message the `message` property value
   * @param validationErrorData the `validationErrorData` property value
   * @param type the `type` property value
   */
  constructor(message: string, validationErrorData: ValidationErrorData[],
              type?: string) {
    super(message, type, validationErrorData);
    this.validationErrorMessage = message;
    this.detail = null;
    this.validationErrorData = validationErrorData;
  }
}

/**
 * An object, containing all data for the particular validation error.
 */
export class ValidationErrorData {
  /**
   * The validation error message.
   */
  message: string;
  /**
   * The parameter name that caused the validation error.
   */
  parameterName?: string;

  /**
   * @param message the `message` property value
   * @param parameterName the `parameterName` property value
   */
  constructor(message: string, parameterName?: string) {
    this.message = message;
    this.parameterName = parameterName;
  }
}

/**
 * The `ConnectClient` constructor options.
 */
export interface ConnectClientOptions {
  /**
   * The `endpoint` property value.
   */
  endpoint?: string;

  /**
   * The `middlewares` property value.
   */
  middlewares?: Middleware[];
}

export interface CallOptions {
  /**
   * Require authentication.
   */
  requireCredentials?: boolean;
}

/**
 * An object with the call arguments and the related Request instance.
 * See also {@link ConnectClient.call | the call() method in ConnectClient}.
 */
export interface MiddlewareContext {
  /**
   * The service class name.
   */
  service: string;

  /**
   * The method name to call on in the service class.
   */
  method: string;

  /**
   * Optional object with method call arguments.
   */
  params?: any;

  /**
   * Client options related with the call.
   */
  options: CallOptions;

  /**
   * The Fetch API Request object reflecting the other properties.
   */
  request: Request;
}

/**
 * An async middleware callback that invokes the next middleware in the chain
 * or makes the actual request.
 * @param context The information about the call and request
 */
export type MiddlewareNext = (context: MiddlewareContext) =>
  Promise<Response> | Response;

/**
 * An async callback function that can intercept the request and response
 * of a call.
 * @param context The information about the call and request
 * @param next Invokes the next in the call chain
 */
export type Middleware = (context: MiddlewareContext, next: MiddlewareNext) =>
  Promise<Response> | Response;

/**
 * Vaadin Connect client class is a low-level network calling utility. It stores
 * an endpoint and facilitates remote calls to services and methods
 * on the Vaadin Connect backend.
 *
 * Example usage:
 *
 * ```js
 * const client = new ConnectClient();
 * const responseData = await client.call('MyVaadinService', 'myMethod');
 * ```
 *
 * ### Endpoint
 *
 * The client supports an `endpoint` constructor option:
 * ```js
 * const client = new ConnectClient({endpoint: '/my-connect-endpoint'});
 * ```
 *
 * The default endpoint is '/connect'.
 *
 */
export class ConnectClient {
  /**
   * The Vaadin Connect backend endpoint
   */
  endpoint: string = '/connect';

  /**
   * The array of middlewares that are invoked during a call.
   */
  middlewares: Middleware[] = [];

  /**
   * @param options Constructor options.
   */
  constructor(options: ConnectClientOptions = {}) {
    if (options.endpoint) {
      this.endpoint = options.endpoint;
    }

    if (options.middlewares) {
      this.middlewares = options.middlewares;
    }
  }

  /**
   * Makes a JSON HTTP request to the `${endpoint}/${service}/${method}` URL,
   * optionally supplying the provided params as a JSON request body,
   * and asynchronously returns the parsed JSON response data.
   *
   * @param service Service class name.
   * @param method Method name to call in the service class.
   * @param params Optional object to be send in JSON request body.
   * @param options Optional client options for this call.
   * @returns {} Decoded JSON response data.
   */
  async call(
    service: string,
    method: string,
    params?: any,
    options: CallOptions = {}
  ): Promise<any> {
    if (arguments.length < 2) {
      throw new TypeError(
        `2 arguments required, but got only ${arguments.length}`
      );
    }

    const headers: Record<string, string> = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };

    // @ts-ignore
    headers['X-CSRF-Token'] = window.Vaadin && window.Vaadin.TypeScript && window.Vaadin.TypeScript.csrfToken || '';

    // helper to keep the undefined value in object after JSON.stringify
    const nullForUndefined = (obj: any): any => {
      for (const property in obj) {
        if (obj[property] === undefined) {
          obj[property] = null;
        }
      }
      return obj;
    }

    const request = new Request(
       `${this.endpoint}/${service}/${method}`, {
         method: 'POST',
         headers,
         body: params !== undefined ? JSON.stringify(nullForUndefined(params)) : undefined
        });

    // The middleware `context`, includes the call arguments and the request
    // constructed from them
    const initialContext: MiddlewareContext = {
      service,
      method,
      params,
      options,
      request
    };

    // The internal middleware to assert and parse the response. The internal
    // response handling should come last after the other middlewares are done
    // with processing the response. That is why this middleware is first
    // in the final middlewares array.
    const responseHandlerMiddleware: Middleware =
      async(
        context: MiddlewareContext,
        next: MiddlewareNext
      ): Promise<Response> => {
        const response = await next(context);
        await assertResponseIsOk(response);
        return response.json();
      };

    // The actual fetch call itself is expressed as a middleware
    // chain item for our convenience. Always having an ending of the chain
    // this way makes the folding down below more concise.
    const fetchNext: MiddlewareNext =
      async(context: MiddlewareContext): Promise<Response> => {
        return fetch(context.request);
      };

    // Assemble the final middlewares array from internal
    // and external middlewares
    const middlewares = [responseHandlerMiddleware].concat(this.middlewares);

    // Fold the final middlewares array into a single function
    const chain = middlewares.reduceRight(
      (next: MiddlewareNext, middleware: Middleware) => {
        // Compose and return the new chain step, that takes the context and
        // invokes the current middleware with the context and the further chain
        // as the next argument
        return (context => middleware(context, next)) as MiddlewareNext;
      },
      // Initialize reduceRight the accumulator with `fetchNext`
      fetchNext
    );

    // Invoke all the folded async middlewares and return
    return chain(initialContext);
  }
}
