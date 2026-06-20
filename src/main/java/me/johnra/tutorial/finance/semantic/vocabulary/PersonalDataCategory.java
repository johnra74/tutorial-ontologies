package me.johnra.tutorial.finance.semantic.vocabulary;

public enum PersonalDataCategory {
    FINANCIAL(Namespaces.DPV_PD + "Financial"),
    EMAIL_ADDRESS(Namespaces.DPV_PD + "EmailAddress"),
    NAME(Namespaces.DPV_PD + "Name"),
    IDENTIFIER(Namespaces.DPV_PD + "Identifier"),
    NONE(null);

    private final String iri;

    PersonalDataCategory(String iri) {
        this.iri = iri;
    }

    public String iri() { return iri; }

    public boolean isPersonalData() { return iri != null; }
}
