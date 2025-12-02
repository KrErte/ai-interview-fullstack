package ee.krerte.aiinterview.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Hetkel ei tee siin CORS-i – kogu CORS on SecurityConfig-is,
 * et vältida konfliktseid konfiguratsioone.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // ei override'i addCorsMappings enam
}
