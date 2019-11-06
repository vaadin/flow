export default interface ExternalDocumentation {
  description: string;
  extensions: { [key: string]: any; };
  url: string;
}