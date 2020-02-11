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
    SimpleTemplateEngine groovyTemplateEngine = { new SimpleTemplateEngine() } ()
    @Lazy
    Template actionInvalidTextTemplate = {
        groovyTemplateEngine.createTemplate(text.getString('action.invalid.text')) } ()
    @Lazy
    Template goInstructionsTemplate = {
        groovyTemplateEngine.createTemplate(text.getString('action.go.instructions.text')) } ()
    @Lazy
    Template roomsummaryTextTemplate = {
        groovyTemplateEngine.createTemplate(text.getString('roomsummary.text')) } ()
    @Lazy
    Template roomsummaryDirectionsTemplate = {
        groovyTemplateEngine.createTemplate(text.getString('roomsummary.directions')) } ()
    @Lazy
    Template requiredPlayersTemplate = {
        groovyTemplateEngine.createTemplate(text.getString('state.players_required.text')) } ()

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
