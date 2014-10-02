package com.moksamedia.grabber

import groovy.json.JsonBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.HTML
import static groovyx.net.http.ContentType.BINARY
import groovyx.net.http.HTTPBuilder

/**
 * Created by cantgetnosleep on 10/1/14.
 */
class ExtractorSpanish {

    def indexes = [

            indicative_div : 6,
            subjunctive_div : 7,
            imperative_div : 8,

            yo_tr : 2,
            tu_tr : 3,
            el_tr : 4,
            nos_tr : 5,
            vos_tr : 6,
            ellos_tr : 7,

            indicative_present_td : 2,
            indicative_preterite_td : 3,
            indicative_imperfect_td : 4,
            indicative_conditional_td : 5,
            indicative_future_td : 6,

            subjunctive_present_td : 2,
            subjunctive_imperfect_td : 3,
            subjunctive_imperfect2_td : 4,
            subjunctive_future_td : 5,

            imperative_imperative_td : 2

    ]

    def html
    def word

    ExtractorSpanish(word) {

        this.word = word

        def http = new HTTPBuilder('http://www.spanishdict.com/')

        html = http.get( path : "/conjugate/$word")

        assert html instanceof groovy.util.slurpersupport.GPathResult
        assert html.HEAD.size() == 1
        assert html.BODY.size() == 1
    }

    String gerundXPath = '/html/body/div[3]/div[2]/div[2]/div[3]/span'
    String participleXPath = '/html/body/div[3]/div[2]/div[2]/div[4]/span'
    String englishXPath = '/html/body/div[3]/div[2]/div[2]/div[1]/div[2]/div'

    def processChromeXPath(String xPath) {
        xPath.replaceAll('/html/', '').replaceAll('/', '.').toUpperCase().replaceAll (/[0-9]{1,2}/) {
            it[0].toInteger() - 1
        }
    }

    def getValueForXPath(def xml, def xPath) {
        xPath = processChromeXPath(xPath)
        Eval.x(xml, "x.${xPath}").text().replaceAll("&nbsp", '')
    }


    def getConjugationPath(mood, tense, person) {

        tense = indexes["${mood}_${tense}_td"]
        mood = indexes["${mood}_div"]
        person = indexes["${person}_tr"]

        "/html/body/div[3]/div[2]/div[2]/div[$mood]/table/tbody/tr[$person]/td[$tense]"

    }

    def getConjugation(mood, tense, person) {
        getValueForXPath(html, getConjugationPath(mood, tense, person))
    }

    def getIrregular(mood, tense, person) {
        def xPath = getConjugationPath(mood, tense, person)
        xPath = processChromeXPath(xPath)
        int numChildren = Eval.x(html, "x.${xPath}").childNodes().size()
        if (numChildren == 1) {
            Eval.x(html, "x.${xPath}.SPAN").text()
        }
        else {
            ""
        }
    }

    def getAll(mood, tense) {
        [
                yo: ["${getConjugation(mood, tense, 'yo')}", "${getIrregular(mood, tense, 'yo')}"],
                tu: ["${getConjugation(mood, tense, 'tu')}", "${getIrregular(mood, tense, 'tu')}"],
                el: ["${getConjugation(mood, tense, 'el')}", "${getIrregular(mood, tense, 'el')}"],
                nos: ["${getConjugation(mood, tense, 'nos')}", "${getIrregular(mood, tense, 'nos')}"],
                vos: ["${getConjugation(mood, tense, 'vos')}", "${getIrregular(mood, tense, 'vos')}"],
                ellos: ["${getConjugation(mood, tense, 'ellos')}", "${getIrregular(mood, tense, 'ellos')}"]
        ]
    }

    def getGerund() {
        getValueForXPath(html, gerundXPath)
    }

    def getParticiple() {
        getValueForXPath(html, participleXPath)
    }

    def getEnglish() {
        getValueForXPath(html, englishXPath).replaceAll("to", "to ")
    }

    def getResultsJson(pretty=false) {
        def data = [
                infinitive: "$word",
                english: "${getEnglish()}",
                gerund: "${getGerund()}",
                participle: "${getParticiple()}",
                indicative: [
                        present: getAll('indicative', 'present'),
                        preterite: getAll('indicative', 'preterite'),
                        imperfect: getAll('indicative', 'imperfect'),
                        conditional: getAll('indicative', 'conditional'),
                        future: getAll('indicative', 'future'),
                ],
                subjunctive: [
                        present: getAll('subjunctive', 'present'),
                        imperfect: getAll('subjunctive', 'imperfect'),
                        imperfect2: getAll('subjunctive', 'imperfect2'),
                        future: getAll('subjunctive', 'future'),
                ],
                imperative: getAll('imperative', 'imperative')
        ]

        def builder = new JsonBuilder(data)

        if (pretty) {
            builder.toPrettyString()
        }
        else {
            builder.toString()
        }

    }

}