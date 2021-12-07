package com.eziosoft.floatzelMongo;

import com.eziosoft.floatzel.Objects.*;
import com.eziosoft.floatzel.SlashCommands.Objects.GuildSlashSettings;
import com.google.gson.Gson;
import com.mongodb.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    private static String slash = "slash";

    @Override
    public void Conninfo(String s) {
        info = g.fromJson(s, ConnInfo.class);
    }

    @Override
    public String getProfile(String s) {
        return g.toJson(utils.getUserFromDbObject(db.getCollection(profile).findOne(new BasicDBObject("_id", s))));
    }

    @Override
    public void saveProfile(String s) {
        User u = g.fromJson(s, User.class);
        db.getCollection(profile).update(new BasicDBObject("_id", u.getUid()), utils.userToDbObject(u));
    }

    @Override
    public void initDatabase() {
        System.out.println("Eziosoft MongoDB Driver starting up...");
        client = new MongoClient(new MongoClientURI("mongodb://" + info.getHost() + ":" + Integer.toString(info.getPort())));
        db = client.getDB("floatzel");
        // thats all for now apparently, mongo will just create the shit as it goes
    }

    @Override
    public boolean checkForUser(String s) {
        boolean heck = db.getCollection(profile).findOne(new BasicDBObject("_id", s)) != null;
        if (!heck){
            db.getCollection(profile).insert(utils.userToDbObject(new User(s)));
        }
        return heck;
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

    @Override
    public void LowLevelDB_Save(Object in, Class<?> type, String table) {
        // check to see if object is of the type we were given
        if (!type.isInstance(in)){
            System.err.println("Error: provided object is not of provided type " + type.getName());
            return;
        }
        // check to see if theres a feild with a defined primary key in it already
        Field primaryField = null;
        for (Field f : type.getDeclaredFields()){
            if (f.getAnnotation(PrimaryKey.class) != null){
                primaryField = f;
                break;
            }
        }
        // if the field is still null, crash
        if (primaryField == null){ throw new RuntimeException("Could not find primary field via annotations!"); }
        // create new DBOBject
        BasicDBObject basic = new BasicDBObject();
        Object fak = null;
        try {
            // loop thru the provided object to get all its values
            for (Field f : type.getDeclaredFields()) {
                // make the field accessible to us!
                f.setAccessible(true);
                // is this field the primary key?
                if (f.get(in) == null){
                    throw new RuntimeException("Error while trying to access field data!");
                }
                if (f.equals(primaryField)) {
                    basic.append("_id", f.get(in));
                    fak = f.get(in);
                } else {
                    // if its not, just use the name of the field for storing it
                    basic.append(f.getName(), f.get(in));
                }
            }
        } catch (Exception e){
            System.err.println("Fatal error while reading object!");
            e.printStackTrace();
            return;
        }
        if (fak == null){
            System.err.println("Error while creating object to query with!");
            return;
        }
        // ok, we have our object, now we have to check to see if we need to insert it or update it
        if (db.getCollection(table).findOne(new BasicDBObject("_id", fak)) == null){
            db.getCollection(table).insert(basic);
        } else {
            db.getCollection(table).update(new BasicDBObject("_id", fak), basic);
        }
    }

    @Override
    public Object LowLevelDB_Load(String table, String pkey, Class<?> type) {
        // first check if theres even an entry for the request in the db. if not, return null
        if (db.getCollection(table).findOne(new BasicDBObject("_id", pkey)) == null){
            return null;
        }
        // next we load the dbobject from the db
        DBObject dbo = db.getCollection(table).findOne(new BasicDBObject("_id", pkey));
        // we need to get the name of the primary key field
        String pfieldname = null;
        for (Field f : type.getDeclaredFields()){
            if (f.getAnnotation(PrimaryKey.class) != null){
                pfieldname = f.getName();
                break;
            }
        }
        if (pfieldname == null){
            System.err.println("could not find primary key field via annotations!");
            return null;
        }
        // make a new object from the provided type
        Object out = null;
        try {
            out = type.getConstructor().newInstance();
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
        try {
            // next, load the values into each field in the object
            for (Field f : type.getDeclaredFields()) {
                // make it accessible
                f.setAccessible(true);
                // is this field the primary key?
                if (f.getName().equals(pfieldname)) {
                    // apparently, it is, so do stuff here
                    f.set(out, dbo.get("_id"));
                } else {
                    Object t = dbo.get(f.getName());
                    if (t instanceof BasicDBList){
                        // oh piss this is a list, first get what type of data goes in the list
                        List<Object> temp = new ArrayList<Object>();
                        for (Object bson : (BasicDBList) t){
                            temp.add(bson);
                        }
                        // store the list in the field depending on what type it is
                        if (f.getGenericType() instanceof ParameterizedType){
                            f.set(out, temp);
                        } else {
                            f.set(out, temp.toArray());
                        }
                    } else {
                        // not a list, store it as-is
                        f.set(out, t);
                    }
                }
                // set it to be non-accesible again
                f.setAccessible(false);
            }
            // return our object
            return out;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
