package me.johnra.tutorial.finance.semantic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "llm.provider=ollama",
    "llm.model=llama3.1",
    "ollama.base-url=http://localhost:11434"
})
class SemanticFinanceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts, beans wire correctly, SHACL shapes load
    }
}
