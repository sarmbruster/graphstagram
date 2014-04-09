package org.neo4j.graphstagram.rest

import org.junit.ClassRule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.helpers.collection.IteratorUtil
import spock.lang.Shared
import spock.lang.Specification

/**
 * short example for a unit test using http://www.spockframework.org
 *
 * N.B. this spawns an ImpermanentGraphDatabase
 */
class PhotoSpec extends Specification {

    @ClassRule
    @Shared
    Neo4jServerResource neo4j = new Neo4jServerResource(
        thirdPartyJaxRsPackages: ["org.neo4j.graphstagram.rest": "/db"]
    )


    def "should add new picture for existing user"() {
        when:
        def response = neo4j.http.PUT("db/photo?name=Sam",
                [raw: getClass().classLoader.getResourceAsStream("darthvader.jpg").bytes ])

        then:
        response.status()==200
//        "START n=node({id}) RETURN n.name AS name".cypher([id:nodeId])[0]['name'] == 'Sam'
    }

/*
    def "should connect 2 users work"() {
        given:
        neo4j.http.PUT("db/user?name=Foo")
        neo4j.http.PUT("db/user?name=Bar")

        when:
        def response = neo4j.http.PUT("db/user/Foo/friend/Bar")

        then:
        response.status()==200

        and:
        IteratorUtil.count( "MATCH (:User {name:{from}})-[r:FRIEND]->(:User {name:{to}}) RETURN r".cypher([from:'Foo', to:'Bar']))==1
    }


    void createUser(String userName) {
        "CREATE (:User {name:{name}})".cypher name:userName
    }
*/
}
