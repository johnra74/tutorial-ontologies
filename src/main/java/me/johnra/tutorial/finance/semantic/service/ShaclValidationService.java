package me.johnra.tutorial.finance.semantic.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShaclValidationService {

    private static final Logger log = LoggerFactory.getLogger(ShaclValidationService.class);

    private final Model shaclShapes;

    public ShaclValidationService(Model shaclShapes) {
        this.shaclShapes = shaclShapes;
    }

    public ValidationResult validate(Model dataGraph) {
        ValidationReport report = ShaclValidator.get().validate(shaclShapes.getGraph(), dataGraph.getGraph());

        if (report.conforms()) {
            log.info("SHACL validation passed");
            return new ValidationResult(true, List.of());
        }

        List<String> violations = report.getEntries().stream()
            .<String>map(e -> "[%s] %s (focus: %s)".formatted(
                e.severity(),
                e.message(),
                e.focusNode()))
            .toList();

        violations.forEach(v -> log.warn("SHACL violation: {}", v));
        return new ValidationResult(false, violations);
    }

    public record ValidationResult(boolean conforms, List<String> violations) {}
}
