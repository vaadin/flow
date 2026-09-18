declare module 'send' {
  import {IncomingMessage, ServerResponse} from 'http';
  import * as mime from 'mime';
  module send {
    interface SendError extends Error {
      status: number;
    }
    interface SendOptions {
      cacheControl? :boolean,
      dotfiles?: string,
      end?: number,
      etag?: boolean,
      extensions?: boolean | Array<string>,
      hidden?: boolean,
      index?: boolean | string,
      lastModified?: boolean,
      maxAge?: number | string,
      root?: string,
      start?: number,

    }
  }
  interface SendStream extends NodeJS.EventEmitter{
    pipe(response: ServerResponse): SendStream;
    type(path: string): SendStream;
    isFresh(): boolean;
  }
  interface Charsets {
    lookup(mime: string): string;
  }
  interface Mime {
    lookup(path: string): string;
    extension(mime: string): string;
    load(filepath: string): void;
    define(mimes: Object): void;

    charsets: Charsets;
    default_type: string;
  }
  interface Send {
    (req: IncomingMessage, path: string, options?: send.SendOptions): SendStream;
    mime: Mime;
  }
  var send: Send;
  export = send;
}
