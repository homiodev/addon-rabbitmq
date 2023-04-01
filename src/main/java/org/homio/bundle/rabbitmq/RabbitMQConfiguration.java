package org.homio.bundle.rabbitmq;

import org.springframework.context.annotation.Configuration;
import org.homio.bundle.api.BundleConfiguration;

@Configuration
@BundleConfiguration
public class RabbitMQConfiguration {

    /*@Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connectionFactory);
        return template;
    }

    @Bean
    public RabbitMessagingTemplate rabbitMessagingTemplate(RabbitTemplate rabbitTemplate) {
        return new RabbitMessagingTemplate(rabbitTemplate);
    }

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties properties,
                                                            ResourceLoader resourceLoader, ObjectProvider<CredentialsProvider>
                                                            credentialsProvider,
                                                            ObjectProvider<CredentialsRefreshService> credentialsRefreshService,
                                                            ObjectProvider<ConnectionNameStrategy> connectionNameStrategy,
                                                            ObjectProvider<ConnectionFactoryCustomizer>
                                                            connectionFactoryCustomizers) throws Exception {
        com.rabbitmq.client.ConnectionFactory connectionFactory = getRabbitConnectionFactoryBean(properties,
                resourceLoader, credentialsProvider, credentialsRefreshService).getObject();
        connectionFactoryCustomizers.orderedStream()
                .forEach((customizer) -> customizer.customize(connectionFactory));
        CachingConnectionFactory factory = new CachingConnectionFactory(connectionFactory);
        PropertyMapper map = PropertyMapper.get();
        map.from(properties::determineAddresses).to(factory::setAddresses);
        map.from(properties::getAddressShuffleMode).whenNonNull().to(factory::setAddressShuffleMode);
        map.from(properties::isPublisherReturns).to(factory::setPublisherReturns);
        map.from(properties::getPublisherConfirmType).whenNonNull().to(factory::setPublisherConfirmType);
        RabbitProperties.Cache.Channel channel = properties.getCache().getChannel();
        map.from(channel::getSize).whenNonNull().to(factory::setChannelCacheSize);
        map.from(channel::getCheckoutTimeout).whenNonNull().as(Duration::toMillis)
                .to(factory::setChannelCheckoutTimeout);
        RabbitProperties.Cache.Connection connection = properties.getCache().getConnection();
        map.from(connection::getMode).whenNonNull().to(factory::setCacheMode);
        map.from(connection::getSize).whenNonNull().to(factory::setConnectionCacheSize);
        map.from(connectionNameStrategy::getIfUnique).whenNonNull().to(factory::setConnectionNameStrategy);
        return factory;
    }

    private RabbitConnectionFactoryBean getRabbitConnectionFactoryBean(RabbitProperties properties,
                                                                       ResourceLoader resourceLoader,
                                                                       ObjectProvider<CredentialsProvider> credentialsProvider,
                                                                       ObjectProvider<CredentialsRefreshService>
                                                                       credentialsRefreshService) {
        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
        factory.setResourceLoader(resourceLoader);
        PropertyMapper map = PropertyMapper.get();
        map.from(properties::determineHost).whenNonNull().to(factory::setHost);
        map.from(properties::determinePort).to(factory::setPort);
        map.from(properties::determineUsername).whenNonNull().to(factory::setUsername);
        map.from(properties::determinePassword).whenNonNull().to(factory::setPassword);
        map.from(properties::determineVirtualHost).whenNonNull().to(factory::setVirtualHost);
        map.from(properties::getRequestedHeartbeat).whenNonNull().asInt(Duration::getSeconds)
                .to(factory::setRequestedHeartbeat);
        map.from(properties::getRequestedChannelMax).to(factory::setRequestedChannelMax);
        RabbitProperties.Ssl ssl = properties.getSsl();
        if (ssl.determineEnabled()) {
            factory.setUseSSL(true);
            map.from(ssl::getAlgorithm).whenNonNull().to(factory::setSslAlgorithm);
            map.from(ssl::getKeyStoreType).to(factory::setKeyStoreType);
            map.from(ssl::getKeyStore).to(factory::setKeyStore);
            map.from(ssl::getKeyStorePassword).to(factory::setKeyStorePassphrase);
            map.from(ssl::getKeyStoreAlgorithm).whenNonNull().to(factory::setKeyStoreAlgorithm);
            map.from(ssl::getTrustStoreType).to(factory::setTrustStoreType);
            map.from(ssl::getTrustStore).to(factory::setTrustStore);
            map.from(ssl::getTrustStorePassword).to(factory::setTrustStorePassphrase);
            map.from(ssl::getTrustStoreAlgorithm).whenNonNull().to(factory::setTrustStoreAlgorithm);
            map.from(ssl::isValidateServerCertificate)
                    .to((validate) -> factory.setSkipServerCertificateValidation(!validate));
            map.from(ssl::getVerifyHostname).to(factory::setEnableHostnameVerification);
        }
        map.from(properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
                .to(factory::setConnectionTimeout);
        map.from(properties::getChannelRpcTimeout).whenNonNull().asInt(Duration::toMillis)
                .to(factory::setChannelRpcTimeout);
        map.from(credentialsProvider::getIfUnique).whenNonNull().to(factory::setCredentialsProvider);
        map.from(credentialsRefreshService::getIfUnique).whenNonNull().to(factory::setCredentialsRefreshService);
        factory.afterPropertiesSet();
        return factory;
    }*/
}
