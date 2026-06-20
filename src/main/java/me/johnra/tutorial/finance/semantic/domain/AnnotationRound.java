package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

public record AnnotationRound(
    int roundNumber,
    List<ColumnAnnotation> proposals,
    ReviewFeedback feedback
) {}
