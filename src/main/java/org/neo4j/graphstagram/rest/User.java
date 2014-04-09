package org.neo4j.graphstagram.rest;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.server.database.CypherExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;
import java.util.Map;

/**
 * provides a REST interface for managing users.
 */
@Path("/user")
public class User
{

    @Context
    protected GraphDatabaseService graphDatabaseService;

    @Context
    protected CypherExecutor cypherExecutor;

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
    public long createFriendship(
            @PathParam("user") String user,
            @PathParam("friend") String friend) {

        ExecutionResult result = cypherExecutor.getExecutionEngine()
            .execute("MATCH (user:User {name:{user}}), (friend:User {name:{friend}}) MERGE (user)-[r:FRIEND]->(friend) RETURN id(r) as id",
                    MapUtil.<String, Object>genericMap("user", user, "friend", friend));
        return (long) IteratorUtil.single(result.columnAs("id"));
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
