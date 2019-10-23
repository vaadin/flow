export default interface ExternalDocumentation {
  description?: string | null;
  extensions?: { [key: string]: any | null; } | null;
  url?: string | null;
}
