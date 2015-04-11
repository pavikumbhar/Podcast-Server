package lan.dk.podcastserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Définition de la configuration associée à la partie persistence
 *
 */

@Configuration
@ComponentScan(basePackages = {"lan.dk.podcastserver.repository", "lan.dk.podcastserver.entity", "lan.dk.podcastserver.business"})
@EnableJpaRepositories("lan.dk.podcastserver.repository")
@EnableTransactionManagement
public class JPAConfig {

    // Hibernate Search :
    private static final String PROPERTY_INDEXMANAGER_NAME = "hibernate.search.default.indexmanager";
    private static final String PROPERTY_INDEXMANAGER_DEFAULT = "near-real-time";

    private static final String PROPERTY_DIRECTORYPROVIDER_NAME = "hibernate.search.default.directory_provider";
    private static final String PROPERTY_DIRECTORYPROVIDER_DEFAULT = "ram";

    private static final String PROPERTY_INDEXBASE_NAME = "hibernate.search.default.indexBase";
    private static final String PROPERTY_INDEXBASE_DEFAULT = "/tmp/lucene";

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddl;

    @Resource
    private Environment environment;

    @Resource DataSource dataSource;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder) throws ClassNotFoundException, SQLException {

        Map<String, Object> jpaProperties = new HashMap<>();

        jpaProperties.put("hibernate.hbm2ddl.auto", ddl);

        // Hibernate Search :
        jpaProperties.put(PROPERTY_INDEXMANAGER_NAME, environment.getProperty(PROPERTY_INDEXMANAGER_NAME, PROPERTY_INDEXMANAGER_DEFAULT));
        jpaProperties.put(PROPERTY_DIRECTORYPROVIDER_NAME, environment.getProperty(PROPERTY_DIRECTORYPROVIDER_NAME, PROPERTY_DIRECTORYPROVIDER_DEFAULT));

        if ("filesystem".equals(environment.getProperty(PROPERTY_DIRECTORYPROVIDER_NAME, PROPERTY_DIRECTORYPROVIDER_DEFAULT))) {
            jpaProperties.put(PROPERTY_INDEXBASE_NAME, environment.getProperty(PROPERTY_INDEXBASE_NAME, PROPERTY_INDEXBASE_DEFAULT));
        }

        return builder
                .dataSource(dataSource)
                .packages("lan.dk.podcastserver.entity")
                .properties(jpaProperties)
                .build();
    }
}
