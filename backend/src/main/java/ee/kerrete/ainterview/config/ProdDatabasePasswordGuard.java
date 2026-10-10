package ee.kerrete.ainterview.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * FP-28: there is no default database password in prod. Runs before any bean is created, so a missing
 * DB_PASSWORD stops startup with a clear message instead of a later "password authentication failed".
 */
@Component
@Profile("prod")
public class ProdDatabasePasswordGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String password = environment.getProperty("spring.datasource.password");
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("DB_PASSWORD is not set. The prod profile has no default database "
                    + "password; set DB_PASSWORD (e.g. in .env for docker compose, see .env.example).");
        }
    }
}
