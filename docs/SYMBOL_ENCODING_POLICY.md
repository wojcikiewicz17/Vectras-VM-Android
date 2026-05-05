# Symbol and Encoding Policy

## Human text and technical identifiers

- Human documentation may contain Portuguese, English, Greek, ideograms and other scripts.
- Technical identifiers must remain ASCII-safe.
- Directionality in multilingual docs must be preserved as text, never converted into technical keys.
- RTL, LTR, vertical and mixed scripts are allowed in prose only.

## Mandatory ASCII zones

The following must be ASCII-only:
- C symbols and headers
- file paths and file names
- JSON keys
- environment variables
- Gradle tasks and artifact names
- shell commands and URLs used in automation

## UTF-8 and web content

- New textual files must be UTF-8 encoded.
- HTML documents must declare UTF-8.
- Do not concatenate raw symbols into URL or GET parameters; use encoded transport.
