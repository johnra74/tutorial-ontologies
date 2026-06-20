package me.johnra.tutorial.finance.semantic.domain;

import me.johnra.tutorial.finance.semantic.vocabulary.LegalBasis;

import java.util.List;

public record DatasetLevelDpv(
    String dataControllerIri,
    List<LegalBasis> legalBases,
    List<String> purposeIris,
    List<String> processingIris,
    List<String> recipientIris,
    List<String> technicalMeasureIris
) {}
