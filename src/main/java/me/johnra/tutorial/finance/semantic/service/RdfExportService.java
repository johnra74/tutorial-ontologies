package me.johnra.tutorial.finance.semantic.service;

import me.johnra.tutorial.finance.semantic.domain.*;
import me.johnra.tutorial.finance.semantic.vocabulary.Namespaces;
import me.johnra.tutorial.finance.semantic.vocabulary.PersonalDataCategory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RdfExportService {

    public Model buildModel(DatasetAnnotation annotation, OdrlAgreementProposal odrl) {
        Model model = ModelFactory.createDefaultModel();
        setPrefixes(model);

        String datasetIri = Namespaces.EX + "dataset";
        Resource dataset = model.createResource(datasetIri);

        addDatasetMetadata(model, dataset, annotation);
        addDpvContext(model, dataset, annotation);
        addDistribution(model, dataset);
        addColumnAnnotations(model, annotation.columnAnnotations());
        addOdrlAgreement(model, dataset, odrl);

        return model;
    }

    public String toTurtle(Model model) {
        var baos = new java.io.ByteArrayOutputStream();
        RDFWriter.create()
            .source(model)
            .format(RDFFormat.TURTLE_PRETTY)
            .output(baos);
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Builds a standalone ODRL Agreement model from an OdrlTermsDocument.
     * This is the rich terms export produced by the TermsAgent; it is
     * serialised separately from the main dataset model.
     */
    public Model buildTermsModel(OdrlTermsDocument terms) {
        Model model = ModelFactory.createDefaultModel();
        setPrefixes(model);
        model.setNsPrefix("gdpr", Namespaces.DPV_GDPR);

        String uid = terms.policyUid() != null
            ? terms.policyUid()
            : Namespaces.EX + "usage-agreement";

        Resource agreement = model.createResource(uid);
        agreement.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Agreement"));

        if (terms.assignerIri() != null)
            agreement.addProperty(prop(model, Namespaces.ODRL + "assigner"),
                model.createResource(terms.assignerIri()));
        if (terms.assigneeIri() != null)
            agreement.addProperty(prop(model, Namespaces.ODRL + "assignee"),
                model.createResource(terms.assigneeIri()));
        if (terms.issuedDate() != null)
            agreement.addProperty(prop(model, Namespaces.DCT + "issued"),
                model.createTypedLiteral(terms.issuedDate(), Namespaces.XSD + "date"));
        if (terms.termEndDateTime() != null)
            agreement.addProperty(prop(model, Namespaces.EX + "termEndDateTime"),
                model.createTypedLiteral(terms.termEndDateTime(), Namespaces.XSD + "dateTime"));
        if (terms.governingLaw() != null)
            agreement.addProperty(prop(model, Namespaces.EX + "governingLaw"),
                terms.governingLaw());
        if (terms.retentionPeriod() != null)
            agreement.addProperty(prop(model, Namespaces.EX + "retentionPeriod"),
                model.createTypedLiteral(terms.retentionPeriod(), Namespaces.XSD + "duration"));
        if (terms.overallNote() != null)
            agreement.addProperty(prop(model, Namespaces.SCHEMA + "description"),
                terms.overallNote());

        // Spatial constraint at policy level
        if (terms.spatialConstraintIri() != null) {
            Resource c = model.createResource();
            c.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Constraint"));
            c.addProperty(prop(model, Namespaces.ODRL + "leftOperand"),
                model.createResource(Namespaces.ODRL + "spatial"));
            c.addProperty(prop(model, Namespaces.ODRL + "operator"),
                model.createResource(Namespaces.ODRL + "eq"));
            c.addProperty(prop(model, Namespaces.ODRL + "rightOperand"),
                model.createResource(terms.spatialConstraintIri()));
            agreement.addProperty(prop(model, Namespaces.ODRL + "constraint"), c);
        }

        // Legal bases
        if (terms.legalBasisIris() != null) {
            terms.legalBasisIris().forEach(iri ->
                agreement.addProperty(prop(model, Namespaces.DPV + "hasLegalBasis"),
                    model.createResource(iri)));
        }

        // Personal data categories
        if (terms.personalDataCategoryIris() != null) {
            terms.personalDataCategoryIris().forEach(iri ->
                agreement.addProperty(prop(model, Namespaces.DPV + "hasPersonalData"),
                    model.createResource(iri)));
        }

        Resource dataset = model.createResource(Namespaces.EX + "dataset");

        // Permissions
        if (terms.permissions() != null) {
            terms.permissions().forEach(perm -> {
                Resource rule = model.createResource();
                rule.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Permission"));
                rule.addProperty(prop(model, Namespaces.ODRL + "target"), dataset);
                if (perm.action() != null)
                    rule.addProperty(prop(model, Namespaces.ODRL + "action"),
                        model.createResource(perm.action()));
                if (perm.purposeConstraintIris() != null) {
                    perm.purposeConstraintIris().forEach(purpose -> {
                        Resource c = model.createResource();
                        c.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Constraint"));
                        c.addProperty(prop(model, Namespaces.ODRL + "leftOperand"),
                            model.createResource(Namespaces.ODRL + "purpose"));
                        c.addProperty(prop(model, Namespaces.ODRL + "operator"),
                            model.createResource(Namespaces.ODRL + "isA"));
                        c.addProperty(prop(model, Namespaces.ODRL + "rightOperand"),
                            model.createResource(purpose));
                        rule.addProperty(prop(model, Namespaces.ODRL + "constraint"), c);
                    });
                }
                if (perm.temporalConstraint() != null) {
                    Resource c = model.createResource();
                    c.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Constraint"));
                    c.addProperty(prop(model, Namespaces.ODRL + "leftOperand"),
                        model.createResource(Namespaces.ODRL + "dateTime"));
                    c.addProperty(prop(model, Namespaces.ODRL + "operator"),
                        model.createResource(Namespaces.ODRL + "lt"));
                    c.addProperty(prop(model, Namespaces.ODRL + "rightOperand"),
                        model.createTypedLiteral(perm.temporalConstraint(),
                            Namespaces.XSD + "dateTime"));
                    rule.addProperty(prop(model, Namespaces.ODRL + "constraint"), c);
                }
                if (perm.duty() != null) {
                    Resource duty = model.createResource();
                    duty.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Duty"));
                    duty.addProperty(prop(model, Namespaces.ODRL + "action"),
                        model.createResource(perm.duty()));
                    rule.addProperty(prop(model, Namespaces.ODRL + "duty"), duty);
                }
                agreement.addProperty(prop(model, Namespaces.ODRL + "permission"), rule);
            });
        }

        // Prohibitions
        if (terms.prohibitions() != null) {
            terms.prohibitions().forEach(prohib -> {
                Resource rule = model.createResource();
                rule.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Prohibition"));
                rule.addProperty(prop(model, Namespaces.ODRL + "target"), dataset);
                if (prohib.action() != null)
                    rule.addProperty(prop(model, Namespaces.ODRL + "action"),
                        model.createResource(prohib.action()));
                if (prohib.rationale() != null && !prohib.rationale().isBlank())
                    rule.addProperty(prop(model, Namespaces.SCHEMA + "description"),
                        prohib.rationale());
                agreement.addProperty(prop(model, Namespaces.ODRL + "prohibition"), rule);
            });
        }

        // Obligations
        if (terms.obligations() != null) {
            terms.obligations().forEach(oblig -> {
                Resource rule = model.createResource();
                rule.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Obligation"));
                rule.addProperty(prop(model, Namespaces.ODRL + "target"), dataset);
                if (oblig.action() != null)
                    rule.addProperty(prop(model, Namespaces.ODRL + "action"),
                        model.createResource(oblig.action()));
                if (oblig.trigger() != null)
                    rule.addProperty(prop(model, Namespaces.EX + "trigger"), oblig.trigger());
                if (oblig.description() != null && !oblig.description().isBlank())
                    rule.addProperty(prop(model, Namespaces.SCHEMA + "description"),
                        oblig.description());
                agreement.addProperty(prop(model, Namespaces.ODRL + "obligation"), rule);
            });
        }

        return model;
    }

    private org.apache.jena.rdf.model.Property prop(Model model, String iri) {
        return model.createProperty(iri);
    }

    private void addDatasetMetadata(Model model, Resource dataset, DatasetAnnotation annotation) {
        DatasetDescriptor desc = annotation.descriptor();

        dataset.addProperty(RDF.type, model.createResource(Namespaces.DCAT + "Dataset"));
        dataset.addProperty(
            model.createProperty(Namespaces.DCT + "title"),
            model.createLiteral(desc.title(), "en"));
        dataset.addProperty(
            model.createProperty(Namespaces.DCT + "publisher"),
            model.createResource(desc.publisherIri()));
        dataset.addProperty(
            model.createProperty(Namespaces.DCT + "license"),
            model.createResource(Namespaces.EX + "licenses/restricted-data-agreement"));
        dataset.addProperty(
            model.createProperty(Namespaces.DCT + "accessRights"),
            model.createResource(Namespaces.ACCESS_RIGHT_RESTRICTED));
        dataset.addProperty(
            model.createProperty(Namespaces.DCT + "conformsTo"),
            model.createResource(Namespaces.EX + "schema/customer-accounts-csvw"));
        dataset.addProperty(
            model.createProperty(Namespaces.DCT + "conformsTo"),
            model.createResource(Namespaces.EX + "profile/finance-restricted"));
        dataset.addProperty(
            model.createProperty(Namespaces.ODRL + "hasPolicy"),
            model.createResource(Namespaces.EX + "usage-agreement"));
    }

    private void addDpvContext(Model model, Resource dataset, DatasetAnnotation annotation) {
        DatasetLevelDpv dpv = annotation.datasetLevelDpv();
        if (dpv == null) return;

        var dpvController = model.createProperty(Namespaces.DPV + "hasDataController");
        var dpvLegalBasis = model.createProperty(Namespaces.DPV + "hasLegalBasis");
        var dpvPurpose    = model.createProperty(Namespaces.DPV + "hasPurpose");
        var dpvPersonalData = model.createProperty(Namespaces.DPV + "hasPersonalData");

        dataset.addProperty(dpvController, model.createResource(dpv.dataControllerIri()));

        dpv.legalBases().forEach(lb ->
            dataset.addProperty(dpvLegalBasis, model.createResource(lb.iri())));

        dpv.purposeIris().forEach(p ->
            dataset.addProperty(dpvPurpose, model.createResource(p)));

        // Collect unique personal data categories from column annotations
        annotation.columnAnnotations().stream()
            .map(ColumnAnnotation::personalDataCategory)
            .filter(PersonalDataCategory::isPersonalData)
            .distinct()
            .forEach(pdc ->
                dataset.addProperty(dpvPersonalData, model.createResource(pdc.iri())));
    }

    private void addDistribution(Model model, Resource dataset) {
        Resource dist = model.createResource(Namespaces.EX + "distribution-service");
        dist.addProperty(RDF.type, model.createResource(Namespaces.DCAT + "Distribution"));

        Resource api = model.createResource(Namespaces.EX + "data-api");
        api.addProperty(RDF.type, model.createResource(Namespaces.DCAT + "DataService"));
        api.addProperty(
            model.createProperty(Namespaces.DCAT + "endpointURL"),
            model.createResource("https://api.example.org/v1/customer-accounts"));
        api.addProperty(
            model.createProperty(Namespaces.DCT + "accessRights"),
            model.createResource(Namespaces.ACCESS_RIGHT_RESTRICTED));

        dist.addProperty(model.createProperty(Namespaces.DCAT + "accessService"), api);
        dist.addProperty(
            model.createProperty(Namespaces.DCT + "conformsTo"),
            model.createResource(Namespaces.EX + "schema/customer-accounts-csvw"));

        dataset.addProperty(model.createProperty(Namespaces.DCAT + "distribution"), dist);
    }

    private void addColumnAnnotations(Model model, List<ColumnAnnotation> annotations) {
        for (ColumnAnnotation ann : annotations) {
            String colIri = Namespaces.EX + "col-" + ann.column().name().replace("_", "-");
            Resource col = model.createResource(colIri);

            col.addProperty(RDF.type, model.createResource(Namespaces.CSVW + "Column"));
            col.addProperty(
                model.createProperty(Namespaces.CSVW + "name"),
                ann.column().name());
            col.addProperty(
                model.createProperty(Namespaces.CSVW + "propertyUrl"),
                ann.propertyUrl());

            if (ann.personalDataCategory().isPersonalData()) {
                col.addProperty(
                    model.createProperty(Namespaces.DPV + "hasPersonalData"),
                    model.createResource(ann.personalDataCategory().iri()));
            }
            if (ann.semanticNote() != null && !ann.semanticNote().isBlank()) {
                col.addProperty(
                    model.createProperty(Namespaces.SCHEMA + "description"),
                    ann.semanticNote());
            }
        }
    }

    private void addOdrlAgreement(Model model, Resource dataset, OdrlAgreementProposal odrl) {
        if (odrl == null) return;

        Resource agreement = model.createResource(Namespaces.EX + "usage-agreement");
        agreement.addProperty(RDF.type, model.createResource(Namespaces.ODRL + "Agreement"));
        agreement.addProperty(
            model.createProperty(Namespaces.ODRL + "assigner"),
            model.createResource(odrl.assignerIri()));
        agreement.addProperty(
            model.createProperty(Namespaces.ODRL + "assignee"),
            model.createResource(odrl.assigneeIri()));

        addOdrlRuleSet(model, agreement, "permission",  odrl.permittedPurposeIris(),  dataset, true);
        addOdrlRuleSet(model, agreement, "prohibition", odrl.prohibitedActionIris(),  dataset, false);
        addOdrlRuleSet(model, agreement, "obligation",  odrl.obligationActionIris(),  dataset, false);
    }

    private void addOdrlRuleSet(Model model, Resource agreement, String ruleType,
                                 List<String> actionIris, Resource target, boolean isPurpose) {
        for (String actionIri : actionIris) {
            Resource rule = model.createResource();
            rule.addProperty(
                model.createProperty(Namespaces.ODRL + "target"), target);
            rule.addProperty(
                model.createProperty(Namespaces.ODRL + "action"),
                model.createResource(actionIri));
            agreement.addProperty(
                model.createProperty(Namespaces.ODRL + ruleType), rule);
        }
    }

    private void setPrefixes(Model model) {
        model.setNsPrefix("dcat",  Namespaces.DCAT);
        model.setNsPrefix("dct",   Namespaces.DCT);
        model.setNsPrefix("csvw",  Namespaces.CSVW);
        model.setNsPrefix("schema", Namespaces.SCHEMA);
        model.setNsPrefix("dpv",   Namespaces.DPV);
        model.setNsPrefix("dpv-pd", Namespaces.DPV_PD);
        model.setNsPrefix("odrl",  Namespaces.ODRL);
        model.setNsPrefix("cc",    Namespaces.CC);
        model.setNsPrefix("ex",    Namespaces.EX);
        model.setNsPrefix("xsd",   Namespaces.XSD);
    }
}
