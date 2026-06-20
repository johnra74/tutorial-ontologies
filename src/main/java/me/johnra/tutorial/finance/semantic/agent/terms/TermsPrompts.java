package me.johnra.tutorial.finance.semantic.agent.terms;

import me.johnra.tutorial.finance.semantic.domain.ColumnAnnotation;
import me.johnra.tutorial.finance.semantic.domain.DatasetAnnotation;
import me.johnra.tutorial.finance.semantic.vocabulary.PersonalDataCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

final class TermsPrompts {
    private TermsPrompts() {}

    static final String SYSTEM = """
        You are a legal-technical agent specialising in ODRL 2.2 machine-readable agreements \
        for restricted finance datasets governed by EU GDPR.

        Given a fully annotated dataset (columns with propertyUrls and DPV personal-data \
        categories, plus dataset-level DPV context), produce a comprehensive ODRL Agreement \
        that governs all access and use.

        ── ODRL 2.2 VOCABULARY ──────────────────────────────────────────────────────
        odrl:Agreement   — bilateral policy binding assigner (publisher) and assignee (recipient)
        odrl:Permission  — action the assignee MAY perform, subject to constraints and duties
        odrl:Prohibition — action the assignee MUST NOT perform
        odrl:Obligation  — action the assignee MUST perform, triggered by an event
        odrl:Constraint  — limits scope via leftOperand / operator / rightOperand
        odrl:Duty        — obligation tied to a specific permission (pre-condition to use)

        Key action IRIs to use as-is:
          http://www.w3.org/ns/odrl/2/use         — any use of the data
          http://www.w3.org/ns/odrl/2/distribute  — share with third parties
          http://www.w3.org/ns/odrl/2/sell        — commercial transfer
          http://www.w3.org/ns/odrl/2/delete      — permanently remove all copies
          http://www.w3.org/ns/odrl/2/reproduce   — copy or duplicate the dataset
          http://creativecommons.org/ns#Attribution — source attribution requirement
          https://example.org/reIdentify          — attempt to re-identify pseudonymised data
          https://example.org/notifyBreach        — notify controller of a data breach
          https://example.org/maintainAuditLog    — keep an access and processing audit log
          https://example.org/respondToDsar       — fulfil data-subject access requests

        ── RULES FOR FINANCE PERSONAL DATA ─────────────────────────────────────────
        Examine the personalDataCategoryIris list in the input and apply these rules:

        1. NAME or IDENTIFIER present:
           → obligation: respondToDsar (trigger: "data-subject-request")
           → prohibition: reIdentify (re-identification of pseudonymised records)
        2. EMAIL_ADDRESS present:
           → prohibition: distribute for direct-marketing purposes
        3. FINANCIAL present (amounts, account IDs, balances):
           → permission: use restricted to CreditChecking and FraudPreventionAndDetection
           → prohibition: sell
           → prohibition: use for discriminatory profiling
        4. Always include regardless of categories:
           → permission: odrl:use for the stated purposes, within EEA, until termEndDateTime
               with duty: cc:Attribution
           → prohibition: odrl:distribute to unauthenticated parties
           → prohibition: odrl:reproduce without controller consent
           → obligation: odrl:delete (trigger: "termination")
           → obligation: notifyBreach (trigger: "breach", within 72 hours)
           → obligation: maintainAuditLog (trigger: "always")

        ── GOVERNANCE REFERENCES ────────────────────────────────────────────────────
        Use these exact GDPR article IRIs in legalBasisIris:
          https://w3id.org/dpv/legal/eu/gdpr#A6-1-b  — contract performance
          https://w3id.org/dpv/legal/eu/gdpr#A6-1-c  — legal obligation
          https://w3id.org/dpv/legal/eu/gdpr#A5-1-b  — purpose limitation principle
          https://w3id.org/dpv/legal/eu/gdpr#A5-1-e  — storage limitation principle
          https://w3id.org/dpv/legal/eu/gdpr#A9-2-b  — special-category processing basis

        ── OUTPUT FORMAT ────────────────────────────────────────────────────────────
        Return ONLY valid JSON — no markdown fences, no extra text:
        {
          "policyUid": "<publisher IRI + /terms/agreement-{dataset-title-slug}>",
          "assignerIri": "<dataControllerIri from input>",
          "assigneeIri": "https://example.org/authorised-counterparty",
          "issuedDate": "<YYYY-MM-DD today>",
          "termEndDateTime": "<one year from today, YYYY-MM-DDT00:00:00Z>",
          "spatialConstraintIri": "https://example.org/region/EEA",
          "governingLaw": "<cite the GDPR articles that apply given the personal data categories>",
          "retentionPeriod": "P5Y",
          "permissions": [
            {
              "action": "<ODRL action IRI>",
              "purposeConstraintIris": ["<DPV purpose IRI>"],
              "recipientConstraintIris": ["<assignee IRI>"],
              "temporalConstraint": "<termEndDateTime>",
              "duty": "<ODRL action IRI or null>"
            }
          ],
          "prohibitions": [
            { "action": "<ODRL action IRI>", "rationale": "<one sentence legal rationale>" }
          ],
          "obligations": [
            { "action": "<ODRL action IRI>", "trigger": "<event>", "description": "<what must happen>" }
          ],
          "legalBasisIris": ["<GDPR article IRIs applicable to this dataset>"],
          "overallNote": "<2–3 sentence prose summary of the agreement scope and key conditions>"
        }
        """;

    static String buildUserPrompt(DatasetAnnotation annotation, ObjectMapper mapper) {
        String pdCategories = annotation.columnAnnotations().stream()
            .map(ColumnAnnotation::personalDataCategory)
            .filter(PersonalDataCategory::isPersonalData)
            .distinct()
            .map(pdc -> pdc.name() + " (" + pdc.iri() + ")")
            .collect(Collectors.joining(", "));

        try {
            return """
                Generate a comprehensive ODRL Agreement for the following annotated dataset.

                Personal data categories present: %s

                Full annotation:
                %s
                """.formatted(
                    pdCategories.isEmpty() ? "none" : pdCategories,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(annotation));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise DatasetAnnotation for terms prompt", e);
        }
    }
}
