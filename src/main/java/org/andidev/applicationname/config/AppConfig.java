package org.andidev.applicationname.config;

import java.util.Properties;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.jadira.usertype.dateandtime.joda.PersistentPeriodAsString;

@Configuration
@ComponentScan(basePackages = {"org.andidev"})
@EnableTransactionManagement
@EnableJpaRepositories("org.andidev.applicationname.repository")
@PropertySource({"application_${spring.profiles.active}.properties"})
@ImportResource({"/WEB-INF/config/security.xml", "/WEB-INF/config/auditing.xml", "/WEB-INF/config/logging.xml", "/WEB-INF/config/jmx.xml", "/WEB-INF/config/monitoring.xml"})
public class AppConfig {

    @Value("${application.environment}")
    private String environment;

    @Inject
    private JpaVendorAdapter jpaVendorAdapter;
    @Inject
    private DataSource dataSource;

    // Properties, nedded for @PropertySource annotation, see https://jira.springsource.org/browse/SPR-8539
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(Environment environment) {
        PropertySourcesPlaceholderConfigurer pspc =
                new PropertySourcesPlaceholderConfigurer();
        Resource[] resources = new ClassPathResource[]{new ClassPathResource("application_"+environment.getProperty("spring.profiles.active") +".properties")};
        pspc.setLocations(resources);
        return pspc;
    }

    // Jdbc Template
    @Bean
    public JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(dataSource);
        return jdbcTemplate;
    }

    
    // persistence.xml Properties
    Properties persistenceXmlProperties() {
      return new Properties() {
         {  // Hibernate
            setProperty("hibernate.hbm2ddl.auto", "create");
            setProperty("hibernate.globally_quoted_identifiers", "true");
            
            // Hibernate Envers
            setProperty("org.hibernate.envers.auditTablePrefix", "");
            setProperty("org.hibernate.envers.auditTableSuffix", "_AUD");
            setProperty("org.hibernate.envers.storeDataAtDelete", "true");
            
            // Use Joda Time
            setProperty("jadira.usertype.autoRegisterUserTypes", "true");
            setProperty("jadira.usertype.databaseZone", "jvm");
         }
      };
   }    
    
    // Entity Manager Factory
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter);
        entityManagerFactory.setJpaProperties(persistenceXmlProperties());
        entityManagerFactory.setPackagesToScan("org.andidev.applicationname.entity");

        return entityManagerFactory;
    }

    // Exception Translation
    @Bean
    public HibernateExceptionTranslator exceptionTranslation() {
        return new HibernateExceptionTranslator();
    }

    // Transaction Manager
    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    // JSR-303 Validation
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean(name = "messageSource")
    public ReloadableResourceBundleMessageSource reloadableMessageSource() {
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("/WEB-INF/messages/messages");
            if ("local".equals(environment)) {
                messageSource.setCacheSeconds(1);
            }
            return messageSource;
    }

    @Bean
    public ImportSql importSql(){
        return new ImportSql();
    }

    
}
