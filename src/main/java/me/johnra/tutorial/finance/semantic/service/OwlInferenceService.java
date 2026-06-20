package me.johnra.tutorial.finance.semantic.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class OwlInferenceService {

    private static final Logger log = LoggerFactory.getLogger(OwlInferenceService.class);

    // DPV class hierarchy: leaf categories → dpv-pd:Identifying → dpv:PersonalData
    // and dpv-pd:Financial → dpv:PersonalData (direct).
    // Loaded inline to avoid a network call; mirrors the published DPV spec.
    private static final String DPV_AXIOMS = """
        @prefix dpv:   <https://w3id.org/dpv#> .
        @prefix dpvpd: <https://w3id.org/dpv/pd#> .
        @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix owl:   <http://www.w3.org/2002/07/owl#> .

        dpv:PersonalData    a owl:Class .
        dpvpd:Identifying   a owl:Class ; rdfs:subClassOf dpv:PersonalData .
        dpvpd:Financial     a owl:Class ; rdfs:subClassOf dpv:PersonalData .
        dpvpd:EmailAddress  a owl:Class ; rdfs:subClassOf dpvpd:Identifying .
        dpvpd:Name          a owl:Class ; rdfs:subClassOf dpvpd:Identifying .
        dpvpd:Identifier    a owl:Class ; rdfs:subClassOf dpvpd:Identifying .
        """;

    // FIBO FBC FinancialAccount is a subclass of the FND Account concept.
    // Mirrors the FIBO ontology structure without requiring the full FIBO download.
    private static final String FIBO_AXIOMS = """
        @prefix fibofbc: <https://spec.edmcouncil.org/fibo/ontology/FBC/ProductsAndServices/FinancialProductsAndServices/> .
        @prefix fibofnd: <https://spec.edmcouncil.org/fibo/ontology/FND/Accounting/AccountingEquity/> .
        @prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix owl:     <http://www.w3.org/2002/07/owl#> .

        fibofnd:Account          a owl:Class .
        fibofbc:FinancialAccount a owl:Class ; rdfs:subClassOf fibofnd:Account .
        """;

    /**
     * Returns an RDFS-inference model backed by the supplied explicit graph plus the
     * DPV and FIBO class-hierarchy axioms. The reasoner closes all rdfs:subClassOf
     * chains, so the returned model contains both explicit and inferred triples.
     */
    public OntModel enrich(Model explicit) {
        OntModel inf = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF);
        inf.read(new StringReader(DPV_AXIOMS),  null, "TURTLE");
        inf.read(new StringReader(FIBO_AXIOMS), null, "TURTLE");
        inf.add(explicit);
        return inf;
    }

    /**
     * Returns IRIs of all DPV personal-data categories applicable to any annotated
     * column — including categories only reachable via rdfs:subClassOf transitivity.
     *
     * Without inference, the SPARQL pattern {@code ?cat rdfs:subClassOf dpv:PersonalData}
     * matches only the direct subclasses in the axiom file: dpv-pd:Financial and
     * dpv-pd:Identifying. Leaf categories like dpv-pd:EmailAddress, dpv-pd:Name, and
     * dpv-pd:Identifier sit one hop further (via dpv-pd:Identifying) and are missed.
     *
     * The OWL_MEM_RDFS_INF reasoner closes the chain, so the same single-hop query
     * returns all five categories correctly.
     */
    public List<String> inferredPersonalDataCategories(Model explicit) {
        OntModel inf = enrich(explicit);

        String sparql = """
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT DISTINCT ?cat WHERE {
                ?col <https://w3id.org/dpv#hasPersonalData> ?cat .
                ?cat rdfs:subClassOf <https://w3id.org/dpv#PersonalData> .
            }
            ORDER BY ?cat
            """;

        List<String> result = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, inf)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                result.add(rs.next().getResource("cat").getURI());
            }
        }

        log.info("OWL  inference: {} personal-data categories (including inferred superclasses): {}",
            result.size(), result);
        return List.copyOf(result);
    }
}
