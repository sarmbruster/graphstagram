package org.neo4j.graphstagram.rest

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.jpeg.JpegDirectory
import org.junit.ClassRule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Transaction
import org.neo4j.visualization.graphviz.GraphvizWriter
import org.neo4j.walk.Walker
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
        setup: "create a user"
        neo4j.http.PUT("db/user?name=Sam")
        neo4j.http.PUT("db/user?name=Joe")

        // make 'em friends
        neo4j.http.PUT("db/user/Sam/friend/Joe")

        when: "add a photo"
        def rawImageData = getClass().classLoader.getResourceAsStream("darthvader.jpg").bytes
        def response = neo4j.http.PUT("db/photo?name=Sam", [raw: rawImageData])

        then:
        response.status()==200

        when: "check if photo has been created"
        def result = "MATCH (:User {name:{name}})-[:SHOOT_PHOTO]->(photo) RETURN photo".cypher([name:'Sam'])

        then:
        result[0]["photo"]

        when:
        response = neo4j.http.GET("db/photo/Joe/friend")

        then:
        response.status()==200
        response.rawContent() == "abc"

    }

    def "processing of image metadata"() {

        when:
        def m = ImageMetadataReader.readMetadata(new BufferedInputStream(getClass().classLoader.getResourceAsStream("darthvader.jpg")), true)
        Directory dir = m.getDirectory(JpegDirectory.class)

        then:
        dir.imageHeight == 126
        dir.imageWidth == 126

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

    def dumpGraphToSvg() {
        Transaction tx = neo4j.graphDatabaseService.beginTx()
        try {
            File dotFile = File.createTempFile("temp", ".dot")
            File svgFile = File.createTempFile("temp", ".svg")
            new GraphvizWriter().emit(dotFile, Walker.fullGraph(neo4j.graphDatabaseService))
            def proc = "/usr/bin/dot -Tsvg ${dotFile.absolutePath}".execute()
            svgFile.withWriter { Writer it -> it << proc.in.text }
            dotFile.delete()
            println svgFile.absolutePath
            tx.success()
        } finally {
            tx.close()
        }
    }

}
