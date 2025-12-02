package ee.krerte.aiinterview.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    /**
     * API key – loetakse kas ENV muutujast või application.yml-ist.
     */
    private String apiKey;

    /**
     * Base URL – vaikimisi https://api.openai.com/v1
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * Mudel, mida kasutada (nt gpt-4.1-mini vms).
     */
    private String model = "gpt-4.1-mini";
}
