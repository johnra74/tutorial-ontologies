package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

public record DatasetAnnotation(
    DatasetDescriptor descriptor,
    List<ColumnAnnotation> columnAnnotations,
    DatasetLevelDpv datasetLevelDpv
) {}
