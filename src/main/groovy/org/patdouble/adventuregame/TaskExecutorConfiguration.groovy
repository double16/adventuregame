package org.patdouble.adventuregame

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.SchedulingTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Configure a Spring managed task executor.
 */
@Configuration
@CompileStatic
@SuppressWarnings('Unused')
class TaskExecutorConfiguration {
    @Bean
    @Qualifier('application')
    @SuppressWarnings('Unused')
    SchedulingTaskExecutor getSchedulingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        // Don't queue up requests, we have some long running threads
        executor.queueCapacity = 0
        executor
    }
}
