package com.moksamedia.grabber

import groovy.json.JsonSlurper

/**
 * Created by cantgetnosleep on 10/1/14.
 */
class Grabber {

    public static void main(String[] args) {

        ExtractorPortuguese extr = new ExtractorPortuguese("pôr")

        def json = extr.getResultsJson(true)

        def result = new JsonSlurper().parseText(json)

        println result.indicative.present.tu
        println result.subjunctive.imperfect.nos

        println json
    }


}
