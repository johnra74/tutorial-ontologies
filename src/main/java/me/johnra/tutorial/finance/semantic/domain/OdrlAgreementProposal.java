package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

public record OdrlAgreementProposal(
    String assignerIri,
    String assigneeIri,
    String termEndDateTime,
    String spatialConstraintIri,
    List<String> permittedPurposeIris,
    List<String> prohibitedActionIris,
    List<String> obligationActionIris
) {}
