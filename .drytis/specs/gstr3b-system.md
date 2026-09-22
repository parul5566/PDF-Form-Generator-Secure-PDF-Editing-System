# GSTR-3B PDF Form Generator — Phase Spec

## Goal
Use the client-provided Form GSTR-3B HTML (3 A4 pages, FILED watermark) as the single source
of truth: parameterize it into a template, generate a matching data-entry UI from the same
field schema, and produce a pixel-faithful PDF on submit.

## Template reference
Client HTML is saved at `/workspace/.drytis/specs/gstr3b-original.html`.
All styling (fonts, borders, watermark rotation, page breaks) must be preserved.

## PDF rendering approach
- Preferred: Java `openhtmltopdf` (HTML+CSS → PDF, supports @page A4).
- If openhtmltopdf cannot render the watermark `transform: rotate()` or layout correctly,
  fallback: headless Chromium print-to-PDF driven from the Spring Boot service.
- Acceptance is visual fidelity: side-by-side with original HTML printed via browser.

## Fields (input schema)
Meta: year, period, GSTIN, legal name, trade name, ARN, ARN date.
Tables 3.1, 3.1.1, 3.2, 4 (ITC A/B/C/D), 5, 5.1, 6.1(A)/(B), breakup, verification
(date, signatory name, designation). Values default "0.00" or "-".
