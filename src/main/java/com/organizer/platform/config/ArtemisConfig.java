package com.organizer.platform.config;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;
import java.util.HashSet;
import java.util.Set;

@org.springframework.context.annotation.Configuration
@EnableJms
public class ArtemisConfig {

    @Value("${spring.artemis.user}")
    private String artemisUser;

    @Value("${spring.artemis.password}")
    private String artemisPassword;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public EmbeddedActiveMQ embeddedActiveMQ() throws Exception {
        Configuration config = new ConfigurationImpl();

        // Basic configuration
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(true);
        config.setJMXManagementEnabled(true);

        // Configure acceptors
        config.addAcceptorConfiguration("in-vm", "vm://0");
        config.addAcceptorConfiguration("tcp", "tcp://0.0.0.0:61616");

        // Configure address settings
        AddressSettings addressSettings = new AddressSettings();
        addressSettings.setAutoCreateQueues(true);
        addressSettings.setAutoCreateAddresses(true);
        addressSettings.setAutoDeleteQueues(false);
        addressSettings.setAutoDeleteAddresses(false);
        addressSettings.setDeadLetterAddress(SimpleString.toSimpleString("DLQ"));
        addressSettings.setExpiryAddress(SimpleString.toSimpleString("ExpiryQueue"));
        config.addAddressesSetting("#", addressSettings);

        // Configure security roles
        Set<Role> roles = new HashSet<>();
        roles.add(new Role("whatsappService", true, true, true, true, true, true, true, true, true, true));
        config.putSecurityRoles("#", roles);

        EmbeddedActiveMQ server = new EmbeddedActiveMQ();
        server.setConfiguration(config);
        server.setSecurityManager(securityManager());
        return server;
    }

    @Bean
    public ActiveMQSecurityManager securityManager() {
        return new ActiveMQSecurityManager() {
            @Override
            public boolean validateUser(String user, String password) {
                return artemisUser.equals(user) && artemisPassword.equals(password);
            }

            @Override
            public boolean validateUserAndRole(String user, String password, Set<Role> roles, CheckType checkType) {
                return validateUser(user, password);
            }
        };
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(
                String.format("vm://0?user=%s&password=%s", artemisUser, artemisPassword)
        );
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setDefaultDestinationName("exampleQueue");
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrency("1-1");
        factory.setSessionTransacted(true);
        return factory;
    }
}