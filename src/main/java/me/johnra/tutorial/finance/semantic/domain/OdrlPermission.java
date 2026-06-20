package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

public record OdrlPermission(
    String action,
    List<String> purposeConstraintIris,
    List<String> recipientConstraintIris,
    String temporalConstraint,
    String duty
) {}
