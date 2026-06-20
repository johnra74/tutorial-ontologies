package me.johnra.tutorial.finance.semantic.agent;

import me.johnra.tutorial.finance.semantic.domain.DataColumn;
import me.johnra.tutorial.finance.semantic.domain.FeedbackItem;

import java.util.List;

public record ProposalInput(
    DataColumn column,
    List<FeedbackItem> priorFeedback
) {
    public static ProposalInput firstRound(DataColumn column) {
        return new ProposalInput(column, List.of());
    }
}
