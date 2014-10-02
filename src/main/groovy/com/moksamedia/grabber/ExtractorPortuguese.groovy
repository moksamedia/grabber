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

class ExtractorPortuguese {

    def indexes = [

            indicative_present_td: 2,
            indicative_preterite_td : 4,
            indicative_imperfect_td : 6,
            indicative_conditional_td : 6,
            indicative_future_td : 4,

            indicative_present_tr_offset: 6,
            indicative_preterite_tr_offset : 6,
            indicative_imperfect_tr_offset : 6,
            indicative_future_tr_offset : 15,
            indicative_conditional_tr_offset : 15,

            subjunctive_present_td : 2,
            subjunctive_imperfect_td : 4,
            subjunctive_future_td : 6,

            subjunctive_present_tr_offset : 24,
            subjunctive_imperfect_tr_offset : 24,
            subjunctive_future_tr_offset : 24,

            imperative_affirmative_td : 1,
            imperative_negative_td : 2,

            imperative_affirmative_tr_offset : 34,
            imperative_negative_tr_offset : 34,

            person_offset_eu : 0,
            person_offset_tu : 1,
            person_offset_ele : 2,
            person_offset_nos : 3,
            person_offset_vos : 4,
            person_offset_eles : 5

    ]

    def html
    def word

    ExtractorPortuguese(word) {

        this.word = word

        def http = new HTTPBuilder('http://www.conjuga-me.net/')

        http.request (GET, BINARY) { req ->

            uri.path = "/en/verbo-$word"

            response.success = { resp, reader ->

                resp.setHeader('Content-Type', 'text/html; charset=ISO-8859-1')

                http.getParser().setDefaultCharset('ISO-8859-1')

                html = http.getParser().parseHTML(resp)

                /*
                    HttpEntity entity = resp.getEntity();
                    if (entity != null) {
                        def results = EntityUtils.toString(entity, "ISO-8859-1")
                        println results

                        XMLReader p = new org.cyberneko.html.parsers.SAXParser();
                        p.setEntityResolver( catalogResolver );
                        return new XmlSlurper( p ).parse( parseText( resp ) );


                        html = new XmlSlurper(false, true, true).parseText(results)
                    }
                */

            }

            response.'404'= { resp ->
                println "Not Found!"
            }

        }

    }


    def getConjugation(mood, tense, person) {
        getValueForXPath(html, getConjugationPath(mood, tense, person))
    }

    def processChromeXPath(String xPath) {
        xPath.replaceAll('/html/', '').replaceAll('/', '.').toUpperCase().replaceAll (/[0-9]+/) {
            it.toInteger() - 1
        }
    }

    def getValueForXPath(def xml, def xPath) {
        xPath = processChromeXPath(xPath)
        //println xPath
        Eval.x(xml, "x.${xPath}").toString()
    }


    def getConjugationPath(mood, tense, person) {

        def offset1 = indexes["${mood}_${tense}_tr_offset"] + indexes["person_offset_${person}"]
        def offset2 = indexes["${mood}_${tense}_td"]

        //println "off1 = $offset1, off2 = $offset2"

        "body/div/table/tbody/tr[$offset1]/td[${offset2}]"

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
                eu: ["${getConjugation(mood, tense, 'eu')}", "${getIrregular(mood, tense, 'eu')}"],
                tu: ["${getConjugation(mood, tense, 'tu')}", "${getIrregular(mood, tense, 'tu')}"],
                ele: ["${getConjugation(mood, tense, 'ele')}", "${getIrregular(mood, tense, 'ele')}"],
                nos: ["${getConjugation(mood, tense, 'nos')}", "${getIrregular(mood, tense, 'nos')}"],
                vos: ["${getConjugation(mood, tense, 'vos')}", "${getIrregular(mood, tense, 'vos')}"],
                eles: ["${getConjugation(mood, tense, 'eles')}", "${getIrregular(mood, tense, 'eles')}"]
        ]
    }

    def getResultsJson(pretty=false) {
        def data = [
                infinitive: "$word",
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
                        future: getAll('subjunctive', 'future'),
                ]
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