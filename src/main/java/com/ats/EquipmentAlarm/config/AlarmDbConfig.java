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
    basePackages = "com.ats.EquipmentAlarm.repo.alarm", // PostgreSQL repositories
    entityManagerFactoryRef = "alarmEntityManagerFactory",
    transactionManagerRef = "alarmTransactionManager"
)
public class AlarmDbConfig {

    @Primary
    @Bean(name = "alarmDataSourceProperties")
    @ConfigurationProperties("spring.datasource.alarm")
    public DataSourceProperties alarmDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "alarmDataSource")
    public DataSource alarmDataSource(@Qualifier("alarmDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "alarmEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean alarmEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("alarmDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.ats.EquipmentAlarm.entity.alarm") // PostgreSQL entities
                .persistenceUnit("alarm")
                .build();
    }

    @Primary
    @Bean(name = "alarmTransactionManager")
    public PlatformTransactionManager alarmTransactionManager(
            @Qualifier("alarmEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
