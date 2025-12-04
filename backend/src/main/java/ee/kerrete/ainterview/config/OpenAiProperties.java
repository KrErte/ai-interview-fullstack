package ee.kerrete.ainterview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenAI.
 *
 * Bound from `application.yml` with prefix: openai.*
 */
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    /**
     * Base URL for OpenAI API.
     * Example: https://api.openai.com/v1
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * API key for OpenAI.
     * Typically injected from env variable OPENAI_API_KEY.
     */
    private String apiKey;

    /**
     * Default model to use, e.g. gpt-4.1-mini.
     */
    private String model = "gpt-4.1-mini";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
