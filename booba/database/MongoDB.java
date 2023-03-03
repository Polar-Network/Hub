package net.polar.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import net.minestom.server.network.player.PlayerConnection;
import net.polar.player.HubPlayer;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public final class MongoDB {

    private static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().create();

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoDB(
        @NotNull String connectionString,
        @NotNull String databaseName,
        @NotNull String collectionName
    ) {
        this.client = MongoClients.create(connectionString);
        MongoDatabase database = this.client.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    public void savePlayer(@NotNull HubPlayer player) {
        PlayerData data = player.getData();
        Map<Integer, Long> checkPoints = data.getParkourCheckPoints();
        Map<Integer, Long> elytraCheckPoints = data.getElytraParkourCheckPoints();

        Document document = new Document("uuid", player.getUuid().toString());
        document.append("normal", GSON.toJson(checkPoints));
        document.append("elytra", GSON.toJson(elytraCheckPoints));
        document.append("normal-best", data.getBestParkourTime());
        document.append("elytra-best", data.getBestElytraParkourTime());
        collection.replaceOne(new Document("uuid", player.getUuid().toString()), document, new ReplaceOptions().upsert(true));
    }

    public HubPlayer loadPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection connection) {
        Document document = collection.find(new Document("uuid", uuid.toString())).first();
        if (document == null) {
            return new HubPlayer(uuid, username, connection, new PlayerData(Map.of(), Map.of(), 0L, 0L));
        }

        final Type type = TypeToken.getParameterized(Map.class, Integer.class, Long.class).getType();

        // Utilizing type tokens to avoid unchecked cast warnings
        Map<Integer, Long> checkPoints = GSON.fromJson(document.getString("normal"), type);
        Map<Integer, Long> elytraCheckPoints = GSON.fromJson(document.getString("elytra"), type);

        Long bestNormal = document.getLong("normal-best");
        Long bestElytra = document.getLong("elytra-best");
        return new HubPlayer(uuid, username, connection, new PlayerData(checkPoints, elytraCheckPoints, bestNormal, bestElytra));
    }

    public void close() {
        client.close();
    }

}
