package org.patdouble.adventuregame

import groovy.transform.CompileDynamic
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.format.FormatterRegistry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Entry point for the web app.
 */
@SpringBootApplication
@EnableScheduling
@CompileDynamic
class AdventuregameApplication implements WebMvcConfigurer {

    static void main(String[] args) {
        SpringApplication.run(AdventuregameApplication, args)
    }

    /**
     * The Spring Boot deps we use provide more than one implementation of FormatterRegistry.
     * @return indeterminate implementation
     */
    @Bean
    @Primary
    FormatterRegistry formatterRegistry(List<FormatterRegistry> beans) {
        return beans.get(0)
    }
}
