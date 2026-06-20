package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

public record DatasetDescriptor(
    String title,
    String publisherIri,
    List<String> purposeIris,
    List<DataColumn> columns
) {}
