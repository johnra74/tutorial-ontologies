package me.johnra.tutorial.finance.semantic.domain;

public record FeedbackItem(
    String columnName,
    Severity severity,
    String message,
    String suggestion
) {
    public enum Severity { ERROR, WARNING, INFO }
}
