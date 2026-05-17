# Java conventions

## Names and imports

Always use simple class names in code, imports, and Javadoc — never
fully-qualified names. Add an import instead.

The only exception is when two classes with the same simple name are
referenced in the same file; then qualify whichever one is less common.

This applies to Javadoc `{@link Foo#bar(Type)}` parameter references too: if
`Type` is imported, write the simple name.

## Text blocks

Use triple quotes (`"""`) for multi-line string blocks in Java text blocks.

## Javadoc

Don't add `@since` tags to Javadoc.

## Calling `executeJs`

When calling `Element.executeJs()`, always pass values as parameters
(`$0`, `$1`, …) — never concatenate them into the expression string.

See [DESIGN_GUIDELINES.md](../../DESIGN_GUIDELINES.md) for the full rules
around wrapping browser APIs from the server side.
