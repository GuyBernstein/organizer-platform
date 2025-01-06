package com.organizer.platform.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.jms.ConnectionFactory;

/**
 * Configuration class for Java Message Service (JMS) components.
 * This configuration is essential for the application's message processing system,
 * particularly for handling WhatsApp messages that require resource-intensive operations like:
 * - Image/document processing
 * - AI service integration
 * - Cloud storage operations
 * - Database transactions
 */
@Configuration
public class JmsConfig {

    /**
     * Constants for thread pool configuration
     */
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 25;
    private static final String THREAD_NAME_PREFIX = "jms-processor-";

    /**
     * Creates and configures the JMS listener container factory.
     * This factory is crucial for the @JmsListener annotations to function properly,
     * particularly in the MessageReceiver class where messages from 'exampleQueue'
     * are processed.
     *
     * @param connectionFactory the JMS connection factory to be used
     * @return configured JMS listener container factory
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setTaskExecutor(messageProcessingExecutor());
        return factory;
    }

    /**
     * Configures the thread pool executor for message processing.
     * This executor manages concurrent processing of messages with controlled
     * thread allocation to prevent resource exhaustion while maintaining
     * optimal throughput.
     *
     * Core pool size: Minimum number of threads kept alive
     * Max pool size: Maximum threads that can be created
     * Queue capacity: Number of messages that can be queued when all threads are busy
     *
     * @return configured thread pool task executor
     */
    @Bean
    public ThreadPoolTaskExecutor messageProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        return executor;
    }
}