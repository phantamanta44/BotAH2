package io.github.phantamanta44.botah2;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.Unirest;
import io.github.phantamanta44.botah2.command.CommandDispatcher;
import io.github.phantamanta44.discord4j.core.Discord;
import io.github.phantamanta44.discord4j.data.wrapper.Bot;
import io.github.phantamanta44.discord4j.data.wrapper.Guild;
import org.apache.http.impl.client.HttpClients;
import sx.blah.discord.Discord4J;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class BotAH {

    private static final File CFG_FILE = new File("botah_cfg.json");
    private static final File DB_FILE = new File("botah_db.json");

    private static JsonObject config, db;
    private static Bot bot;
    private static CommandDispatcher commander;

    public static void main(String[] args) {
        JsonParser parser = new JsonParser();
        try (BufferedReader in = new BufferedReader(new FileReader(CFG_FILE))) {
            config = parser.parse(in).getAsJsonObject();
        } catch (IOException e) {
            Discord.logger().error("Failed to read config!");
            fail(e, -1);
        }
        if (DB_FILE.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(DB_FILE))) {
                db = parser.parse(in).getAsJsonObject();
            } catch (IOException e) {
                Discord.logger().error("Failed to read bot data!");
                fail(e, -1);
            }
        }
        else
            db = new JsonObject();
        ((Logger) Discord4J.LOGGER).setLevel(Level.INFO);
        Discord.authenticate(config.get("token").getAsString(), config.get("ownerId").getAsString())
                .fail(e -> {
                    Discord.logger().error("Authentication failed!");
                    fail(e, 34);
                })
                .done(b -> {
                    bot = b;
                    commander = new CommandDispatcher(b.eventBus(), b.moduleMan());
                    doSslHack();
                    Discord.logger().info("Initialization complete.");
                });
    }

    public static CommandDispatcher commander() {
        return commander;
    }

    public static JsonObject config() {
        return config;
    }

    public static JsonObject data() {
        return db;
    }

    public static void saveDb() {
        try (PrintWriter out = new PrintWriter(new FileWriter(DB_FILE))) {
            out.println(new Gson().toJson(db));
        } catch (IOException e) {
            Discord.logger().warn("Failed to write bot data!");
            e.printStackTrace();
        }
    }

    public static Bot bot() {
        return bot;
    }

    private static void doSslHack() {
        try {
            TrustManager[] hackyThing = new TrustManager[] {
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
                        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
                        @Override public X509Certificate[] getAcceptedIssuers() { return null; }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, hackyThing, new SecureRandom());
            Unirest.setHttpClient(HttpClients.custom().setSSLContext(sc).build());
        } catch (Exception e) {
            Discord.logger().error("Failed to run hacky SSL thing!");
            e.printStackTrace();
        }
    }

    private static void fail(Throwable e, int code) {
        e.printStackTrace();
        Runtime.getRuntime().exit(code);
    }

    public static JsonObject guildCfg(Guild guild) {
        JsonObject guilds = guildSection();
        if (!guilds.has(guild.id())) {
            JsonObject gElem = new JsonObject();
            gElem.addProperty("prefix", CommandDispatcher.globalPrefix());
            guilds.add(guild.id(), gElem);
            return gElem;
        }
        return guilds.get(guild.id()).getAsJsonObject();
    }

    private static JsonObject guildSection() {
        if (!data().has("guilds"))
            data().add("guilds", new JsonObject());
        return data().get("guilds").getAsJsonObject();
    }

    public static boolean lenientMatch(String matcher, String toTest) {
        return matcher.equalsIgnoreCase(toTest)
                || matcher.replaceAll("\\s", "").equalsIgnoreCase(toTest)
                || matcher.replaceAll("[^\\s\\d]", "").equalsIgnoreCase(toTest)
                || matcher.replaceAll("\\W", "").equalsIgnoreCase(toTest);
    }

}
