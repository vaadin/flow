export default interface XML {
  attribute: boolean;
  extensions: { [key: string]: any; };
  name: string;
  namespace: string;
  prefix: string;
  wrapped: boolean;
}