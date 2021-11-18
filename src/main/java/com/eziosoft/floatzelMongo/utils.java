package com.eziosoft.floatzelMongo;

import com.eziosoft.floatzel.Objects.Stock;
import com.eziosoft.floatzel.Objects.Tweet;
import com.eziosoft.floatzel.Objects.User;
import com.eziosoft.floatzel.SlashCommands.Objects.GuildSlashSettings;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class utils {

    public static DBObject userToDbObject(User u){
        return new BasicDBObject("_id", u.getUid())
                .append("bal", u.getBal())
                .append("lastloan", u.getLastloan())
                .append("perms", u.getPerms())
                .append("stockid", u.getStockid())
                .append("isAdmin", u.isAdmin());
    }
    public static User getUserFromDbObject(DBObject o){
        User temp = new User(
                (String) o.get("_id"),
                (int) o.get("bal"),
                (long) o.get("lastloan"),
                new Boolean[]{false, false},
                (int) o.get("stockid"),
                (boolean) o.get("isAdmin")
        );
        BasicDBList list = (BasicDBList) o.get("perms");
        int i = 0;
        Boolean[] blist = temp.getPerms();
        for (Object bson : list){
            if (bson instanceof Boolean){
                blist[i] = (Boolean) bson;
                i++;
            }
        }
        temp.setPerms(blist);
        return temp;
    }

    public static DBObject stockToDbObject(Stock s){
        return new BasicDBObject("_id", s.getId())
                .append("name", s.getName())
                .append("units", s.getUnits())
                .append("price", s.getPrice())
                .append("diff", s.getDiff());
    }
    public static Stock dbObjectToStock(DBObject dbo){
        return new Stock(
                (int) dbo.get("_id"),
                (String) dbo.get("name"),
                (int) dbo.get("units"),
                (int) dbo.get("price"),
                (int) dbo.get("diff")
        );
    }

    public static DBObject tweetToDbObject(Tweet t){
        // do stuff here
        return new BasicDBObject("_id", t.getId()).append("text", t.getText());
    }
    public static Tweet dbObjectToTweet(DBObject db){
        return new Tweet(
                (String) db.get("text"),
                (int) db.get("_id")

        );
    }

    public static DBObject slashSettingsToDbObject(GuildSlashSettings gss){
        return new BasicDBObject("_id", gss.getGuildid())
                .append("registered", gss.getRegistered());
    }
    public static GuildSlashSettings dbObjectToGSS(DBObject dbo){
        GuildSlashSettings gss = new GuildSlashSettings((String) dbo.get("_id"));
        for (Object o : (BasicDBList) dbo.get("registered")){
            if (o instanceof String){
                gss.addRegistered((String) o);
            }
        }
        return gss;
    }
}
