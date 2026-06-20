package me.johnra.tutorial.finance.semantic.domain;

import java.util.ArrayList;
import java.util.List;

public class AnnotationSession {
    private final String sessionId;
    private final List<AnnotationRound> rounds = new ArrayList<>();
    private DatasetAnnotation finalAnnotation;
    private OdrlTermsDocument termsDocument;
    private boolean approved;
    private boolean shaclConforms;
    private List<String> inferredPersonalDataCategories = List.of();

    public AnnotationSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String sessionId() { return sessionId; }
    public List<AnnotationRound> rounds() { return List.copyOf(rounds); }
    public DatasetAnnotation finalAnnotation() { return finalAnnotation; }
    public OdrlTermsDocument termsDocument() { return termsDocument; }
    public boolean approved() { return approved; }
    public boolean shaclConforms() { return shaclConforms; }
    public List<String> inferredPersonalDataCategories() { return inferredPersonalDataCategories; }

    public void addRound(AnnotationRound round) { rounds.add(round); }

    public void complete(DatasetAnnotation annotation, boolean approvedAndConforms) {
        this.finalAnnotation = annotation;
        this.approved = approvedAndConforms;
        this.shaclConforms = approvedAndConforms;
    }

    public void setTermsDocument(OdrlTermsDocument termsDocument) {
        this.termsDocument = termsDocument;
    }

    public void setInferredPersonalDataCategories(List<String> categories) {
        this.inferredPersonalDataCategories = categories != null ? categories : List.of();
    }
}
