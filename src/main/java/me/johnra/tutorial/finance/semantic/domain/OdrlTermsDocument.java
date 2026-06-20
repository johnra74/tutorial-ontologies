package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

/**
 * Rich ODRL Agreement produced by the TermsAgent after annotation convergence.
 * Supersedes the inline OdrlAgreementProposal from the ReviewerAgent for the
 * final export; the reviewer's simpler proposal is still used for SHACL checks
 * during the feedback loop.
 */
public record OdrlTermsDocument(
    String policyUid,
    String assignerIri,
    String assigneeIri,
    String issuedDate,
    String termEndDateTime,
    String spatialConstraintIri,
    String governingLaw,
    String retentionPeriod,
    List<OdrlPermission>  permissions,
    List<OdrlProhibition> prohibitions,
    List<OdrlObligation>  obligations,
    List<String> legalBasisIris,
    List<String> personalDataCategoryIris,
    String overallNote
) {}
