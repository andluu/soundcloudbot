package com.github.andluu.config;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(basePackages = "com.github.andluu.repositories")
@EnableTransactionManagement
public class DataConfig {

    @Bean
    @DependsOn("h2Server") // uncomment this if h2 console feature is enabled
    public DataSource dataSource() {
        // JDBC settings
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:h2:file:~/soundcloud_tg_bot;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE");
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("additionalProperties") Properties props) {
        // JPA settings
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("com.github.andluu.model");
        factory.setDataSource(dataSource());
        factory.setJpaProperties(props);
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        // Enable TxManager
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public Properties additionalProperties(BotConfig config) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", config.getDdl());
        return properties;
    }

    // ----------For Debugging: H2 Console feature----------
    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn("h2WebServer")
    public org.h2.tools.Server h2Server() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public org.h2.tools.Server h2WebServer() throws SQLException {
        return Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
    }

}
