package me.johnra.tutorial.finance.semantic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Semantic Finance Annotation API")
                .description("""
                    Multi-agent API for producing DCAT + CSV-W + FIBO + DPV + ODRL + SHACL-compliant
                    semantic annotations for restricted finance datasets.

                    Two AI agents collaborate in a feedback loop:
                    - **ProposalAgent** — maps each CSV column to a schema.org / FIBO propertyUrl and DPV-PD privacy category.
                    - **ReviewerAgent** — reviews the full dataset view for consistency, SHACL compliance, and proposes the ODRL Agreement.

                    The loop runs until the reviewer approves **and** Apache Jena SHACL validation passes.
                    """)
                .version("0.1.0")
                .contact(new Contact()
                    .name("Finance Data Governance Team")
                    .email("data-governance@example.org"))
                .license(new License()
                    .name("Restricted — see ODRL Agreement")
                    .url("https://example.org/licenses/restricted-data-agreement")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local development")));
    }
}
