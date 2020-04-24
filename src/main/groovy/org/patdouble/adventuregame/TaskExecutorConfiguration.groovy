package org.patdouble.adventuregame

import groovy.transform.CompileStatic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.SchedulingTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@CompileStatic
class TaskExecutorConfiguration {
    @Bean
    SchedulingTaskExecutor getSchedulingTaskExecutor() {
        new ThreadPoolTaskExecutor()
    }
}
