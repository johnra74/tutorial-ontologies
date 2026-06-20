package me.johnra.tutorial.finance.semantic.domain;

import java.util.List;

public record ReviewFeedback(
    boolean approved,
    String overallNote,
    List<FeedbackItem> items,
    OdrlAgreementProposal odrlAgreement
) {
    public boolean hasErrors() {
        return items != null && items.stream()
            .anyMatch(i -> i.severity() == FeedbackItem.Severity.ERROR);
    }
}
