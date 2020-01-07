package org.patdouble.adventuregame.i18n

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.CompileDynamic
import groovy.transform.Memoized

/**
 * Resources for natural language text input and output.
 */
@CompileDynamic
class Bundles {
    @Memoized(protectedCacheSize = 5)
    static Bundles get(Locale locale) {
        new Bundles(locale)
    }

    final Locale locale
    @Lazy
    SimpleTemplateEngine groovyTemplateEngine = { new SimpleTemplateEngine() }()
    @Lazy
    Template actionInvalidTextTemplate = {
        groovyTemplateEngine.createTemplate(getText().getString('action.invalid.text')) }()
    @Lazy
    Template goInstructionsTemplate = {
        groovyTemplateEngine.createTemplate(getText().getString('action.go.instructions.text')) }()
    @Lazy
    Template roomsummaryTextTemplate = {
        groovyTemplateEngine.createTemplate(getText().getString('roomsummary.text')) }()
    @Lazy
    Template roomsummaryDirectionsTemplate = {
        groovyTemplateEngine.createTemplate(getText().getString('roomsummary.directions')) }()

    private Bundles(Locale locale) {
        this.locale = locale
    }

    ResourceBundle getActions() {
        ResourceBundle.getBundle('i18n.actions', locale)
    }

    ResourceBundle getText() {
        ResourceBundle.getBundle('i18n.text', locale)
    }
}
