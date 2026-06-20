package me.johnra.tutorial.finance.semantic.vocabulary;

public enum OdrlAction {
    USE(Namespaces.ODRL + "use"),
    SELL(Namespaces.ODRL + "sell"),
    DISTRIBUTE(Namespaces.ODRL + "distribute"),
    DELETE(Namespaces.ODRL + "delete"),
    REPRODUCE(Namespaces.ODRL + "reproduce"),
    ATTRIBUTION(Namespaces.CC + "Attribution"),
    RE_IDENTIFY(Namespaces.EX + "reIdentify"),
    NOTIFY_BREACH(Namespaces.EX + "notifyBreach"),
    MAINTAIN_AUDIT_LOG(Namespaces.EX + "maintainAuditLog"),
    RESPOND_TO_DSAR(Namespaces.EX + "respondToDsar");

    private final String iri;

    OdrlAction(String iri) {
        this.iri = iri;
    }

    public String iri() { return iri; }
}
