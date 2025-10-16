package com.ats.EquipmentAlarm.config;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.ats.EquipmentAlarm.repo.position", // MSSQL repos only
    entityManagerFactoryRef = "positionEntityManagerFactory",
    transactionManagerRef = "positionTransactionManager"
)
public class PositionDbConfig {

    @Bean(name = "positionDataSourceProperties")
    @ConfigurationProperties("spring.datasource.position")
    public DataSourceProperties positionDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "positionDataSource")
    public DataSource positionDataSource(@Qualifier("positionDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "positionEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean positionEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("positionDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.ats.EquipmentAlarm.entity.position") // MSSQL entities
                .persistenceUnit("position")
                .build();
    }

    @Bean(name = "positionTransactionManager")
    public PlatformTransactionManager positionTransactionManager(
            @Qualifier("positionEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
