# Semantic Finance — Multi-Agent Dataset Annotation

A Spring Boot 3.3 / Java 21 application that automatically produces DCAT + CSV-W + FIBO + DPV + ODRL + SHACL-compliant semantic annotations for restricted finance datasets using a two-agent AI feedback loop.

## Overview

The application implements the layered semantic architecture described in `finance-dataset-semantic-spec.md`. Two AI agents collaborate iteratively:

- **ProposalAgent** — receives one CSV column at a time and proposes a `propertyUrl` (schema.org or FIBO), a DPV-PD personal-data category, and a semantic note.
- **ReviewerAgent** — receives the full dataset annotation, checks cross-column consistency and SHACL profile compliance, and proposes the ODRL Agreement. Returns structured feedback that the ProposalAgent uses to adjust.

The loop runs until the ReviewerAgent approves **and** Apache Jena SHACL validation passes as a hard gate (default: up to 5 rounds).

<svg width="100%" viewBox="0 0 680 624" role="img" style="" xmlns="http://www.w3.org/2000/svg">
<title style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">Annotation orchestration flow</title>
<desc style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">A POST request enters the annotation orchestrator, which calls the proposal and reviewer LLM agents and then a SHACL hard gate in a loop. On failure the round repeats; once the result is approved and conforms, an annotation session is written with Turtle export.</desc>
<defs>
<marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M2 1L8 5L2 9" fill="none" stroke="context-stroke" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path></marker>
</defs>

<g style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">
  <rect x="220" y="40" width="240" height="44" rx="8" stroke-width="0.5" style="fill:rgb(241, 239, 232);stroke:rgb(95, 94, 90);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
  <text x="340" y="62" text-anchor="middle" dominant-baseline="central" style="fill:rgb(68, 68, 65);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:14px;font-weight:500;text-anchor:middle;dominant-baseline:central">POST /api/v1/annotations</text>
</g>

<line x1="340" y1="84" x2="340" y2="120" marker-end="url(#arrow)" style="fill:none;stroke:rgb(115, 114, 108);color:rgb(0, 0, 0);stroke-width:1.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></line>

<g style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">
  <rect x="220" y="124" width="240" height="56" rx="8" stroke-width="0.5" style="fill:rgb(241, 239, 232);stroke:rgb(95, 94, 90);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
  <text x="340" y="145" text-anchor="middle" dominant-baseline="central" style="fill:rgb(68, 68, 65);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:14px;font-weight:500;text-anchor:middle;dominant-baseline:central">Annotation orchestrator</text>
  <text x="340" y="163" text-anchor="middle" dominant-baseline="central" style="fill:rgb(95, 94, 90);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:middle;dominant-baseline:central">runs the round loop</text>
</g>

<line x1="340" y1="180" x2="340" y2="216" marker-end="url(#arrow)" style="fill:none;stroke:rgb(115, 114, 108);color:rgb(0, 0, 0);stroke-width:1.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></line>

<g style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">
  <rect x="220" y="220" width="240" height="56" rx="8" stroke-width="0.5" style="fill:rgb(238, 237, 254);stroke:rgb(83, 74, 183);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
  <text x="340" y="241" text-anchor="middle" dominant-baseline="central" style="fill:rgb(60, 52, 137);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:14px;font-weight:500;text-anchor:middle;dominant-baseline:central">Proposal agent</text>
  <text x="340" y="259" text-anchor="middle" dominant-baseline="central" style="fill:rgb(83, 74, 183);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:middle;dominant-baseline:central">column × N, in parallel</text>
</g>

<line x1="340" y1="276" x2="340" y2="312" marker-end="url(#arrow)" style="fill:none;stroke:rgb(115, 114, 108);color:rgb(0, 0, 0);stroke-width:1.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></line>

<g style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">
  <rect x="220" y="316" width="240" height="56" rx="8" stroke-width="0.5" style="fill:rgb(238, 237, 254);stroke:rgb(83, 74, 183);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
  <text x="340" y="337" text-anchor="middle" dominant-baseline="central" style="fill:rgb(60, 52, 137);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:14px;font-weight:500;text-anchor:middle;dominant-baseline:central">Reviewer agent</text>
  <text x="340" y="355" text-anchor="middle" dominant-baseline="central" style="fill:rgb(83, 74, 183);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:middle;dominant-baseline:central">feedback + ODRL terms</text>
</g>

<line x1="340" y1="372" x2="340" y2="408" marker-end="url(#arrow)" style="fill:none;stroke:rgb(115, 114, 108);color:rgb(0, 0, 0);stroke-width:1.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></line>

<g style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">
  <rect x="220" y="412" width="240" height="56" rx="8" stroke-width="1" style="fill:rgb(250, 238, 218);stroke:rgb(133, 79, 11);color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
  <text x="340" y="433" text-anchor="middle" dominant-baseline="central" style="fill:rgb(99, 56, 6);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:14px;font-weight:500;text-anchor:middle;dominant-baseline:central">SHACL hard gate</text>
  <text x="340" y="451" text-anchor="middle" dominant-baseline="central" style="fill:rgb(133, 79, 11);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:middle;dominant-baseline:central">Jena · pass or fail</text>
</g>

<path d="M220 440 L150 440 L150 248 L220 248" fill="none" marker-end="url(#arrow)" style="fill:none;stroke:rgb(115, 114, 108);color:rgb(0, 0, 0);stroke-width:1.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></path>
<text x="140" y="340" text-anchor="end" dominant-baseline="central" style="fill:rgb(61, 61, 58);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:end;dominant-baseline:central">↻ on fail</text>

<line x1="340" y1="468" x2="340" y2="504" marker-end="url(#arrow)" style="fill:none;stroke:rgb(115, 114, 108);color:rgb(0, 0, 0);stroke-width:1.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></line>
<text x="352" y="488" text-anchor="start" dominant-baseline="central" style="fill:rgb(61, 61, 58);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:start;dominant-baseline:central">approved &amp;&amp; conforms</text>

<g style="fill:rgb(0, 0, 0);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto">
  <rect x="220" y="508" width="240" height="56" rx="8" stroke-width="0.5" style="fill:rgb(241, 239, 232);stroke:rgb(95, 94, 90);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
  <text x="340" y="529" text-anchor="middle" dominant-baseline="central" style="fill:rgb(68, 68, 65);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:14px;font-weight:500;text-anchor:middle;dominant-baseline:central">Annotation session</text>
  <text x="340" y="547" text-anchor="middle" dominant-baseline="central" style="fill:rgb(95, 94, 90);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:middle;dominant-baseline:central">rounds + Turtle export</text>
</g>

<rect x="196" y="584" width="12" height="12" rx="3" stroke-width="0.5" style="fill:rgb(238, 237, 254);stroke:rgb(83, 74, 183);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
<text x="214" y="591" text-anchor="start" dominant-baseline="central" style="fill:rgb(61, 61, 58);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:start;dominant-baseline:central">LLM agents</text>
<rect x="320" y="584" width="12" height="12" rx="3" stroke-width="0.5" style="fill:rgb(250, 238, 218);stroke:rgb(133, 79, 11);color:rgb(0, 0, 0);stroke-width:0.5px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:16px;font-weight:400;text-anchor:start;dominant-baseline:auto"></rect>
<text x="338" y="591" text-anchor="start" dominant-baseline="central" style="fill:rgb(61, 61, 58);stroke:none;color:rgb(0, 0, 0);stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;opacity:1;font-family:&quot;Anthropic Sans&quot;, -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, sans-serif;font-size:12px;font-weight:400;text-anchor:start;dominant-baseline:central">deterministic gate</text>
</svg>

## Semantic layers covered

| Layer | Standard | Handled by |
|-------|----------|------------|
| Catalog & dataset metadata | DCAT-AP | `RdfExportService` |
| Logical structure | CSV-W `tableSchema` | `RdfExportService` |
| Domain semantics | schema.org + FIBO FND/BE/FBC | `ProposalAgent` |
| Privacy & lawful use | DPV + DPV-PD | `ProposalAgent` + `RdfExportService` |
| Terms of condition | ODRL Agreement | `ReviewerAgent` |
| Validation | SHACL | `ShaclValidationService` (Jena) |

## Requirements

- Java 21+
- Maven 3.9+
- One of:
  - [Ollama](https://ollama.com) running locally with a pulled model (default: `llama3.1`)
  - OpenAI API key
  - Anthropic API key

## Configuration

Provider is selected in `src/main/resources/application.yml`:

```yaml
llm:
  provider: ollama          # ollama | openai | anthropic
  model: llama3.1           # any model name the provider accepts

ollama:
  base-url: http://localhost:11434

openai:
  base-url: https://api.openai.com
  api-key: ${OPENAI_API_KEY:}

anthropic:
  api-key: ${ANTHROPIC_API_KEY:}

annotation:
  max-rounds: 5
```

Override at runtime with environment variables or system properties:

```bash
# OpenAI
OPENAI_API_KEY=sk-... mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=openai --llm.model=gpt-4o"

# Anthropic
ANTHROPIC_API_KEY=sk-ant-... mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=anthropic --llm.model=claude-opus-4-8"
```

## Running

```bash
# With Ollama (default)
ollama pull llama3.1
mvn spring-boot:run

# Build a fat JAR
mvn package
java -jar target/semantic-finance-0.1.0-SNAPSHOT.jar
```

## API

### Annotate a dataset

```
POST /api/v1/annotations
Content-Type: application/json
```

Request body:

```json
{
  "datasetTitle": "Customer accounts master",
  "publisherIri": "https://example.org/org-acme-bank",
  "purposeIris": [
    "https://w3id.org/dpv#CreditChecking",
    "https://w3id.org/dpv#FraudPreventionAndDetection"
  ],
  "columns": [
    { "name": "customer_id", "datatype": "string" },
    { "name": "lei",         "datatype": "string" },
    { "name": "legal_name",  "datatype": "string" },
    { "name": "email",       "datatype": "string" },
    { "name": "account_id",  "datatype": "string" },
    { "name": "account_type","datatype": "string" },
    { "name": "balance",     "datatype": "decimal" },
    { "name": "currency",    "datatype": "string" },
    { "name": "opened_date", "datatype": "date" },
    { "name": "jurisdiction","datatype": "string" }
  ]
}
```

Response (abbreviated):

```json
{
  "sessionId": "3fa85f64-...",
  "totalRounds": 2,
  "approved": true,
  "shaclConforms": true,
  "rounds": [ ... ],
  "finalAnnotation": { ... },
  "turtle": "@prefix dcat: ... \nex:dataset a dcat:Dataset ; ..."
}
```

### Retrieve a session

```
GET /api/v1/annotations/{sessionId}
```

### Re-run SHACL validation on a session

```
POST /api/v1/annotations/{sessionId}/validate
```

```json
{
  "sessionId": "3fa85f64-...",
  "conforms": true,
  "violations": []
}
```

## Project structure

```
src/main/java/me/johnra/tutorial/finance/semantic/
├── agent/
│   ├── Agent.java                  — generic Agent<I,O> interface
│   ├── ProposalInput.java
│   ├── proposal/
│   │   ├── ProposalAgent.java      — column-level annotation proposals
│   │   └── ProposalPrompts.java
│   └── review/
│       ├── ReviewerAgent.java      — full-dataset review + ODRL proposal
│       └── ReviewerPrompts.java
├── domain/                         — immutable records
├── vocabulary/
│   ├── Namespaces.java             — all IRI prefix constants
│   ├── PersonalDataCategory.java   — DPV-PD enum (carries IRI)
│   ├── LegalBasis.java             — DPV legal basis enum
│   └── OdrlAction.java             — ODRL action enum
├── service/
│   ├── AnnotationOrchestrator.java — multi-agent loop (virtual threads)
│   ├── ShaclValidationService.java — Jena SHACL hard gate
│   ├── RdfExportService.java       — Turtle serialisation
│   └── llm/
│       ├── LlmClient.java          — provider interface
│       ├── OllamaLlmClient.java
│       ├── OpenAiLlmClient.java
│       └── AnthropicLlmClient.java
├── api/
│   └── AnnotationController.java
└── config/
    ├── LlmConfig.java              — conditional bean per provider
    └── JenaConfig.java             — loads SHACL shapes from classpath
src/main/resources/
├── application.yml
└── shacl/finance-shapes.ttl        — four NodeShapes from spec §11
```

## Design principles

**SOLID**
- *SRP* — each class has one job: propose, review, orchestrate, validate, or export.
- *OCP* — `Agent<I,O>` interface lets new agent types be added without touching the orchestrator.
- *LSP* — `ProposalAgent` and `ReviewerAgent` are interchangeable `Agent` implementations.
- *ISP* — `LlmClient` exposes one method; SHACL and RDF export are separate services.
- *DIP* — all dependencies injected via constructor; `LlmClient` and Jena `Model` as Spring beans.

**DRY**
- All vocabulary IRIs live in `Namespaces.java` — never hardcoded elsewhere.
- `PersonalDataCategory`, `LegalBasis`, and `OdrlAction` enums carry their own IRIs.
- Prompt templates centralised in `ProposalPrompts` / `ReviewerPrompts`.
- A single `LlmClient.complete()` call path covers all three providers.

## License

Restricted — see `finance-dataset-semantic-spec.md §9` for the ODRL Agreement terms.
