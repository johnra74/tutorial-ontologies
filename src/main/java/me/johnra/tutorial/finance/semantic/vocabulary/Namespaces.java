package me.johnra.tutorial.finance.semantic.vocabulary;

public final class Namespaces {
    private Namespaces() {}

    public static final String EX      = "https://example.org/";
    public static final String DCAT    = "http://www.w3.org/ns/dcat#";
    public static final String DCT     = "http://purl.org/dc/terms/";
    public static final String CSVW    = "http://www.w3.org/ns/csvw#";
    public static final String SCHEMA  = "http://schema.org/";
    public static final String DPV     = "https://w3id.org/dpv#";
    public static final String DPV_PD  = "https://w3id.org/dpv/pd#";
    public static final String DPV_GDPR = "https://w3id.org/dpv/legal/eu/gdpr#";
    public static final String ODRL    = "http://www.w3.org/ns/odrl/2/";
    public static final String CC      = "http://creativecommons.org/ns#";
    public static final String SH      = "http://www.w3.org/ns/shacl#";
    public static final String XSD     = "http://www.w3.org/2001/XMLSchema#";
    public static final String RDF     = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    // FIBO modules
    public static final String FIBO_FND_ACC  = "https://spec.edmcouncil.org/fibo/ontology/FND/Accounting/CurrencyAmount/";
    public static final String FIBO_FND_PEO  = "https://spec.edmcouncil.org/fibo/ontology/FND/AgentsAndPeople/People/";
    public static final String FIBO_BE_LEI   = "https://spec.edmcouncil.org/fibo/ontology/BE/LegalEntities/LEIEntities/";
    public static final String FIBO_FBC_SVC  = "https://spec.edmcouncil.org/fibo/ontology/FBC/ProductsAndServices/FinancialProductsAndServices/";

    // Commonly used full IRIs
    public static final String ACCESS_RIGHT_RESTRICTED =
        "http://publications.europa.eu/resource/authority/access-right/RESTRICTED";
}
