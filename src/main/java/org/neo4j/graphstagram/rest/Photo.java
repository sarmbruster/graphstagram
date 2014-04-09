package org.neo4j.graphstagram.rest;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.jpeg.JpegDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.server.database.CypherExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * provides a REST interface for managin photos.
 */
@Path("/photo")
public class Photo
{

    // where we store images
    static final String FILE_STORAGE = "filestorage";

    @Context
    protected GraphDatabaseService graphDatabaseService;

    @Context
    protected CypherExecutor cypherExecutor;

    @PUT
    public long addPhoto(@QueryParam("name") String name, Map<String, Object> payload) throws ImageProcessingException, IOException, MetadataException {

        byte[] raw = DatatypeConverter.parseBase64Binary((String) payload.get("raw"));
        String fileName = UUID.randomUUID().toString();
        File imageFile = new File(FILE_STORAGE, fileName);

        // write image to secondary datastore (filesystem)
        FileUtils.writeByteArrayToFile(imageFile, raw);

        // extract some metadata
        BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(raw));

        Metadata metadata = ImageMetadataReader.readMetadata(stream, true);
        JpegDirectory directory = metadata.getDirectory(JpegDirectory.class);

        // build map for metadata to be stored in graph
        Map<String, Object> params = MapUtil.genericMap(
                "width", directory.getImageWidth(),
                "height", directory.getImageHeight(),
                "url", imageFile.toURI().toURL().toString()
        );

        // write metadata to the graph
        ExecutionResult result = cypherExecutor.getExecutionEngine()
                .execute("MATCH (user:User {name:{name}}) CREATE (user)-[:SHOOT_PHOTO]->(photo:Photo {params}) return id(photo) as id",
                        MapUtil.<String, Object>genericMap("name", name, "params", params));
        return (long) IteratorUtil.single(result.columnAs("id"));
    }

    @GET
    @Path("{userName}/friend")
    public Map<String, Collection<Map<String, Object>>> getPhotosOfFriendsWithLevel(
            @PathParam("userName") String userName,
            @QueryParam("level") @DefaultValue("1") int level
    ) throws IOException {

        try (Transaction tx = graphDatabaseService.beginTx()) {
            StringBuilder cypher = new StringBuilder();
            cypher.append("MATCH (:User {name: {userName}})-[:FRIEND*").append(level).append("]-(friend)-[:SHOOT_PHOTO]->(photo) RETURN friend.name, photo");
            ExecutionResult result = cypherExecutor.getExecutionEngine().execute(cypher.toString(), Collections.<String, Object>singletonMap("userName", userName));

            Map<String,Collection<Map<String,Object>>> retVal = new HashMap<>();

            for (Map<String,Object> row: result) {
                String friend = (String) row.get("friend.name");

                Collection<Map<String,Object>> photos = retVal.get(friend);
                if (photos==null) {
                    photos = new ArrayList<>();
                    retVal.put(friend, photos);
                }

                Map<String,Object> photoData = new HashMap<>();

                Node photo = (Node) row.get("photo");
                for (String key: photo.getPropertyKeys()) {
                    Object value = photo.getProperty(key);
                    if (key.equals("url")) {
                        // amend graph search result with contents from secondary datasource (filesystem)
                        InputStream is = new URL(value.toString()).openStream();
                        byte[] raw = IOUtils.toByteArray(is);
                        photoData.put(key, DatatypeConverter.printBase64Binary(raw));
                    } else {
                        photoData.put(key, value);
                    }

                }
                photos.add(photoData);
            }
            tx.success();
            return retVal;
        }
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
