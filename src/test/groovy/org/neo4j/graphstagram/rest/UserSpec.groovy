package org.neo4j.graphstagram.rest

import org.junit.ClassRule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.Index
import spock.lang.Shared
import spock.lang.Specification

/**
 * short example for a unit test using http://www.spockframework.org
 *
 * N.B. this spawns an ImpermanentGraphDatabase
 */
class UserSpec extends Specification {

    @ClassRule
    @Shared
    Neo4jServerResource neo4j = new Neo4jServerResource(
        thirdPartyJaxRsPackages: ["org.neo4j.graphstagram.rest": "/db"]
    )

    def "should trivial test work"() {
        when:
        def response = neo4j.http.GET("db/user/test")

        then:
        response.status()==200
        response.entity=="test"
    }

    def "should return empty collection for empty database"() {
        when:
        def response = neo4j.http.GET("db/user")

        then:
        response.status()==200
        response.entity=="null"
    }

    def "should return a valid user list"() {

        given:
        createUser("Stefan")
        createUser("Thomas")

        when:
        def response = neo4j.http.GET("db/user")

        then:
        response.status()==200
        response.entity=='["Stefan","Thomas"]'

    }

    void createUser(String userName) {
        "CREATE (:User {name:{name}})".cypher name:userName
    }
}
