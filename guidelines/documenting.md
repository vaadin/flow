# Documenting

## Wrap thin; document thick

Java API wrapping a browser/JS API must be written for Java developers who
do not know the underlying JS API. Explain: what the method does in Java
terms; when to call it; what the parameters and return value mean;
threading and lifecycle expectations; any browser-specific caveats
(e.g. "Safari always returns UNKNOWN"). Do not assume the reader will
read the W3C spec or the `.ts` source.

## Javadoc

- Javadoc for public API explains the *why* and the caveats, not just the
  type signature.
- Call out reliability concerns prominently — if a value is best-effort,
  say so, and enumerate the browsers where it degrades.
- Prefer short, runnable examples in the class-level Javadoc. Keep
  examples consistent with the real platform APIs (e.g. if you show
  `map.setCenter(...)`, use the real Vaadin Map `Coordinate(longitude,
  latitude)` shape).
- **Do not add `@since` tags.**
- Javadoc describes the code today, not what changed. Change history
  belongs in commit messages.

