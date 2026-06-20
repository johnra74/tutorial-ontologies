package me.johnra.tutorial.finance.semantic.domain;

import me.johnra.tutorial.finance.semantic.vocabulary.PersonalDataCategory;

public record ColumnAnnotation(
    DataColumn column,
    String propertyUrl,
    String vocabulary,
    PersonalDataCategory personalDataCategory,
    String semanticNote
) {}
