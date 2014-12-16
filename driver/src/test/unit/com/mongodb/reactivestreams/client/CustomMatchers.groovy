/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.reactivestreams.client

import org.hamcrest.BaseMatcher
import org.hamcrest.Description

class CustomMatchers {

    static isTheSameAs(final Object e) {
        [
                matches         : { a -> compare(e, a) },
                describeTo      : { Description description -> description.appendText("Operation has the same attributes ${e.class.name}")
                },
                describeMismatch: { a, description -> describer(e, a, description) }
        ] as BaseMatcher
    }

    static compare(expected, actual) {
        if (expected == actual) {
            return true
        }
        if (expected == null || actual == null) {
            return false
        }
        if (actual.class.name != expected.class.name) {
            return false
        }
        actual.class.declaredFields.findAll { !it.synthetic }*.name.collect { it ->
            if (it == 'decoder') {
                return actual.decoder.class == expected.decoder.class
            } else if (actual."$it" != expected."$it") {
                def (a1, e1) = [actual."$it", expected."$it"]
                if (List.isCase(a1) && List.isCase(e1) && (a1.size() == e1.size())) {
                    def i = -1
                    return a1.collect { a -> i++; compare(a, e1[i]) }.every { it }
                }
                return false
            }
            true
        }.every { it }
    }

    static describer(expected, actual, description) {
        if (expected == actual) {
            return true
        }
        if (expected == null || actual == null) {
            description.appendText("different values: $expected != $actual, ")
            return false
        }
        if (actual.class.name != expected.class.name) {
            description.appendText("different classes: ${expected.class.name} != ${actual.class.name}, ")
            return false
        }
        actual.class.declaredFields.findAll { !it.synthetic }*.name
                .collect { it ->
            if (it == 'decoder' && actual.decoder.class != expected.decoder.class) {
                description.appendText("different decoder classes $it : ${expected.decoder.class.name} != ${actual.decoder.class.name}, ")
                return false
            } else if (actual."$it" != expected."$it") {
                def (a1, e1) = [actual."$it", expected."$it"]
                if (List.isCase(a1) && List.isCase(e1) && (a1.size() == e1.size())) {
                    def i = -1
                    a1.each { a ->
                        i++; if (!compare(a, e1[i])) {
                            describer(a, e1[i], description)
                        }
                    }.every { it }
                }
                description.appendText("different values in $it : $e1 != $a1\n")
                return false
            }
            true
        }
    }
}
