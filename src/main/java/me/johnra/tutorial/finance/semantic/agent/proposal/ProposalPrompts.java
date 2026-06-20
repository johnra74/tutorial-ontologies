package me.johnra.tutorial.finance.semantic.agent.proposal;

import me.johnra.tutorial.finance.semantic.domain.DataColumn;
import me.johnra.tutorial.finance.semantic.domain.FeedbackItem;

import java.util.List;
import java.util.stream.Collectors;

final class ProposalPrompts {
    private ProposalPrompts() {}

    static final String SYSTEM = """
        You are a semantic data architect specialising in finance datasets.
        Given a CSV column name and datatype, you assign the best semantic annotation
        drawn from the following vocabularies:

        schema.org (prefix "schema:"): http://schema.org/
        FIBO FND (prefix "fibo-fnd:"): https://spec.edmcouncil.org/fibo/ontology/FND/
        FIBO BE  (prefix "fibo-be:"):  https://spec.edmcouncil.org/fibo/ontology/BE/
        FIBO FBC (prefix "fibo-fbc:"): https://spec.edmcouncil.org/fibo/ontology/FBC/

        Well-known mappings for finance datasets:
        - customer_id   → schema:identifier (SCHEMA_ORG, IDENTIFIER personal data)
        - lei           → fibo-be:hasLegalEntityIdentifier (FIBO_BE, no personal data)
        - legal_name    → fibo-fnd:hasLegalName (FIBO_FND, NAME personal data)
        - email         → schema:email (SCHEMA_ORG, EMAIL_ADDRESS personal data)
        - account_id    → fibo-fbc:hasAccountIdentifier (FIBO_FBC, FINANCIAL personal data)
        - account_type  → schema:additionalType (SCHEMA_ORG, no personal data)
        - balance       → fibo-fnd:hasAmount (FIBO_FND, FINANCIAL personal data)
        - currency      → fibo-fnd:hasCurrency (FIBO_FND, no personal data)
        - opened_date   → schema:dateCreated (SCHEMA_ORG, no personal data)
        - jurisdiction  → schema:spatialCoverage (SCHEMA_ORG, no personal data)

        Respond ONLY with a JSON object — no markdown fences, no extra text:
        {
          "propertyUrl": "<full IRI of the semantic property>",
          "vocabulary": "<SCHEMA_ORG | FIBO_FND | FIBO_BE | FIBO_FBC>",
          "personalDataCategory": "<FINANCIAL | EMAIL_ADDRESS | NAME | IDENTIFIER | NONE>",
          "semanticNote": "<one sentence explaining the mapping choice>"
        }
        """;

    static String buildUserPrompt(DataColumn column, List<FeedbackItem> priorFeedback) {
        var sb = new StringBuilder();
        sb.append("Column name: ").append(column.name()).append("\n");
        sb.append("Datatype: ").append(column.datatype()).append("\n");

        if (!priorFeedback.isEmpty()) {
            sb.append("\nPrior reviewer feedback for this column (address each point):\n");
            for (FeedbackItem item : priorFeedback) {
                sb.append("- [").append(item.severity()).append("] ")
                  .append(item.message());
                if (item.suggestion() != null && !item.suggestion().isBlank()) {
                    sb.append(" Suggestion: ").append(item.suggestion());
                }
                sb.append("\n");
            }
        }

        sb.append("\nReturn the JSON annotation for this column.");
        return sb.toString();
    }
}
