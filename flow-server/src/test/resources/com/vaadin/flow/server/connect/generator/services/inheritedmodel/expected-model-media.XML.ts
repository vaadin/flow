export default interface XML {
  attribute?: boolean | null;
  extensions?: { [key: string]: any | null; } | null;
  name?: string | null;
  namespace?: string | null;
  prefix?: string | null;
  wrapped?: boolean | null;
}
