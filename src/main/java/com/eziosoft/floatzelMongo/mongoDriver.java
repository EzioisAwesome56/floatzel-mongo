package com.eziosoft.floatzelMongo;

import com.eziosoft.floatzel.Objects.*;
import com.eziosoft.floatzel.SlashCommands.Objects.GuildSlashSettings;
import com.google.gson.Gson;
import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class mongoDriver implements GenaricDatabase {

    private static MongoClient client;
    private static DB db;
    private static ConnInfo info;
    private static final Gson g = new Gson();
    // strings because i will forget what goes where otherwise!
    private static String profile = "profile";
    private static String stock = "stock";
    private static String tweets = "tweets";
    private static String perm = "permission";
    private static String slash = "slash";

    @Override
    public void Conninfo(String s) {
        info = g.fromJson(s, ConnInfo.class);
    }

    @Override
    public String getProfile(String s) {
        return g.toJson(utils.getUserFromDbObject(db.getCollection(profile).findOne(new BasicDBObject("_id", g.fromJson(s, User.class).getUid()))));
    }

    @Override
    public void saveProfile(String s) {
        User u = g.fromJson(s, User.class);
        db.getCollection(profile).update(new BasicDBObject("_id", u.getUid()), utils.userToDbObject(u));
    }

    @Override
    public void initDatabase() {
        System.out.println("Eziosoft MongoDB Driver starting up...");
        try {
            client = new MongoClient(new MongoClientURI("mongodb://" + info.getHost() + ":" + Integer.toString(info.getPort())));
            db = client.getDB("floatzel");
            // thats all for now apparently, mongo will just create the shit as it goes
        } catch (UnknownHostException e) {
            System.err.println("FATAL ERROR: an error has occurred while trying to connect to mongodb!");
            e.printStackTrace();
            System.exit(-2);
        }
    }

    @Override
    public boolean checkForUser(String s) {
        return db.getCollection(profile).findOne(new BasicDBObject("_id", g.fromJson(s, User.class).getUid())) != null;
    }

    @Override
    public int totalStocks() {
        return (int) db.getCollection(stock).count();
    }

    @Override
    public void makeNewStock(String s) {
        db.getCollection(stock).insert(utils.stockToDbObject(g.fromJson(s, Stock.class)));
    }

    @Override
    public void updateStock(String s) {
        Stock st = g.fromJson(s, Stock.class);
        db.getCollection(stock).update(new BasicDBObject("_id", st.getId()), utils.stockToDbObject(st));
    }

    @Override
    public String getStock(int i) {
        return g.toJson(utils.dbObjectToStock(db.getCollection(stock).findOne(new BasicDBObject("_id", i))));
    }

    @Deprecated
    @Override
    public void makeTable(String s, String s1) {
        // mongodb doesnt need this
        return;
    }

    @Override
    public int totalTweets() {
        return (int) db.getCollection(tweets).count();
    }

    @Override
    public void saveTweet(String s) {
        db.getCollection(tweets).insert(utils.tweetToDbObject(g.fromJson(s, Tweet.class)));
    }

    @Override
    public String loadTweet(int i) {
        return g.toJson(utils.dbObjectToTweet(db.getCollection(tweets).findOne(new BasicDBObject("_id", i))));
    }

    @Override
    public boolean checkForStock(int i) {
        return db.getCollection(stock).findOne(new BasicDBObject("_id", i)) != null;
    }

    @Override
    public void deleteStock(int i) {
        db.getCollection(stock).remove(new BasicDBObject("_id", i));
    }

    @Override
    public void setPerm(String s, int i) {
        User u = g.fromJson(getProfile(s), User.class);
        Boolean[] temp = u.getPerms();
        temp[i - 1] = true;
        u.setPerms(temp);
        saveProfile(g.toJson(u));
    }

    @Override
    public void saveSlashGuildSettings(GuildSlashSettings guildSlashSettings) {
        if (db.getCollection(slash).findOne(new BasicDBObject("_id", guildSlashSettings.getGuildid())) == null){
            // insert a new one
            db.getCollection(slash).insert(utils.slashSettingsToDbObject(guildSlashSettings));
        } else {
            // update the old one
            db.getCollection(slash).update(new BasicDBObject("_id", guildSlashSettings.getGuildid()), utils.slashSettingsToDbObject(guildSlashSettings));
        }
    }

    @Override
    public GuildSlashSettings[] loadAllSlashSettings() {
        List<GuildSlashSettings> temp = new ArrayList<GuildSlashSettings>();
        DBCursor cur = db.getCollection(slash).find();
        while (cur.hasNext()){
            temp.add(utils.dbObjectToGSS(cur.next()));
        }
        return temp.toArray(new GuildSlashSettings[]{});
    }
}
