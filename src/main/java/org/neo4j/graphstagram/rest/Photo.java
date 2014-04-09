package org.neo4j.graphstagram.rest;

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
import java.util.Iterator;
import java.util.Map;

/**
 * provides a REST interface for managin photos.
 */
@Path("/photo")
public class Photo
{

    @Context
    protected GraphDatabaseService graphDatabaseService;

    @Context
    protected CypherExecutor cypherExecutor;

/*

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
*/

    @PUT
    public long addPhoto(@QueryParam("name") String name, Map<String, Object> payload) {
        ExecutionResult result = cypherExecutor.getExecutionEngine()
                .execute("CREATE (user:User {name:{name}}) return id(user) as id",
                        MapUtil.<String, Object>genericMap("name", name));
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
