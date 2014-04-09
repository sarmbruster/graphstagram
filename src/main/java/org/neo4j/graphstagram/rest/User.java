package org.neo4j.graphstagram.rest;

import org.apache.commons.collections.CollectionUtils;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.server.database.CypherExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * graphstagram for Neo4j rest extensions
 * <p/>
 * provides a REST interface for maintaining users.
 */
@Path("/user")
public class User
{

    @Context
    protected GraphDatabaseService graphDatabaseService;

    @Context
    protected CypherExecutor cypherExecutor;

    @GET
    @Path("/test")
    @Produces("text/plain")
    public long doTest()
    {

        try (Transaction tx = graphDatabaseService.beginTx())
        {
            Index<Node> index = graphDatabaseService.index().forNodes( "users" );

            Node node = graphDatabaseService.createNode();
            String userName = "123";
            node.setProperty( "username", userName );
            index.add( node, "username", node.getProperty( "username" ) );
            tx.success();
            return node.getId();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Iterator<Object> getUsernames(
            @DefaultValue("0") @QueryParam("skip") int skip,
            @DefaultValue("20") @QueryParam("limit") int limit) {
        Map<String,Object> params = MapUtil.<String,Object>genericMap("skip", skip, "limit", limit);
        ExecutionResult result =  cypherExecutor.getExecutionEngine()
                .execute("MATCH (user:User) RETURN user.name SKIP {skip} LIMIT {limit}", params);
        return result.columnAs("user.name");
    }

    @PUT
    public long createUser(@QueryParam("name") String name) {
        ExecutionResult result = cypherExecutor.getExecutionEngine()
                .execute("CREATE (user:User {name:{name}}) return id(user) as id",
                        MapUtil.<String, Object>genericMap("name", name));
        return (long) IteratorUtil.single(result.columnAs("id"));
    }

    @PUT
    @Path("{user}/friend/{friend}")
    public void createFriendship(
            @PathParam("user") String user,
            @PathParam("friend") String friend) {

        cypherExecutor.getExecutionEngine()
            .execute("CREATE UNIQUE (:User {name:{user}})-[:FRIEND]->(:User {name:{friend})",
                    MapUtil.<String, Object>genericMap("user", user, "friend", friend));
    }


    /**
     * to be used from unit tests
     *
     * @param graphDatabaseService
     */
    public void setGraphDatabaseService( GraphDatabaseService graphDatabaseService )
    {
        this.graphDatabaseService = graphDatabaseService;
    }

    /**
     * to be used from unit tests
     *
     * @param cypherExecutor
     */
    public void setCypherExecutor( CypherExecutor cypherExecutor )
    {
        this.cypherExecutor = cypherExecutor;
    }
}
