package me.johnra.tutorial.finance.semantic.config;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class JenaConfig {

    @Bean
    public Model shaclShapes() {
        Model shapes = ModelFactory.createDefaultModel();
        try (InputStream is = getClass().getResourceAsStream("/shacl/finance-shapes.ttl")) {
            if (is == null) {
                throw new IllegalStateException("SHACL shapes file not found on classpath: /shacl/finance-shapes.ttl");
            }
            RDFDataMgr.read(shapes, is, Lang.TURTLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SHACL shapes", e);
        }
        return shapes;
    }
}
