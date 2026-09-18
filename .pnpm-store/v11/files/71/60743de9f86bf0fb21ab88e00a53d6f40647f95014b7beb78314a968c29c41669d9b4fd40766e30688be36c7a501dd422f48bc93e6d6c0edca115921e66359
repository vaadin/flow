declare module 'whatwg-url' {
  export function parseURL(
      input: string, options?: {baseURL?: URL, encodingOverride?: string}): URL|
      null;

  // https://url.spec.whatwg.org/#url-class
  export interface URL {
    scheme: string;
    username: string;
    password: string;
    host: string;
    port: number|null;
    path: string[];
    query: string|null;
    fragment: string|null;
    cannotBeABaseURL: boolean;
  }
}
