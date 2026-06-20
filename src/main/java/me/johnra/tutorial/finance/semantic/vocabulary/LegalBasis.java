package me.johnra.tutorial.finance.semantic.vocabulary;

public enum LegalBasis {
    CONTRACT(Namespaces.DPV + "Contract"),
    LEGAL_OBLIGATION(Namespaces.DPV + "LegalObligation"),
    GDPR_A6_1B(Namespaces.DPV_GDPR + "A6-1-b"),
    GDPR_A6_1C(Namespaces.DPV_GDPR + "A6-1-c");

    private final String iri;

    LegalBasis(String iri) {
        this.iri = iri;
    }

    public String iri() { return iri; }
}
