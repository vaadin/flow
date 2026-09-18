declare module 'vinyl-fs-fake' {
  import * as vinyl from 'vinyl-fs';
  import * as stream from 'stream';

  export interface File {
    path: string;
    contents: string;
  }
  export interface Options { cwdbase: boolean; }
  export function src(files: File[], options?: Options): stream.Readable;
}
