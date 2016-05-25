package net.oneandone.pommes.cli;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.util.Strings;

import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class Parser implements AutoCloseable {
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public static void run(Node listing, Node root, BlockingQueue<Node> dest) throws Exception {
        String uri;
        long size;
        Date lastModified;
        String sha1;
        int count;

        count = 0;
        try (InputStream is = listing.newInputStream(); Parser parser = new Parser(Json.createParser(is))) {
            parser.next(JsonParser.Event.START_OBJECT);
            parser.eatKeyValueString("uri");
            parser.eatKeyValueString("created");
            parser.eatKey("files");
            parser.next(JsonParser.Event.START_ARRAY);
            while (parser.next() == JsonParser.Event.START_OBJECT) {
                uri = parser.eatKeyValueString("uri");
                size = parser.eatKeyValueNumber("size");
                try {
                    lastModified = FMT.parse(parser.eatKeyValueString("lastModified"));
                } catch (ParseException e) {
                    throw new IllegalStateException();
                }
                parser.eatKeyValueFalse("folder");
                parser.eatKeyValueString("sha1");
                if (uri.endsWith(".pom")) {
                    dest.put(root.join(Strings.removeLeft(uri, "/")));
                }
                if (parser.eatTimestampsOpt() != JsonParser.Event.END_OBJECT) {
                    throw new IllegalStateException();
                }
                count++;
                if (count % 100 == 0) {
                    System.out.print(".");
                }
                if (count % 10000 == 0) {
                    System.out.println();
                }
            }
        }
    }

    private final JsonParser parser;

    public Parser(JsonParser parser) {
        this.parser = parser;
    }

    public JsonParser.Event next() {
        return parser.next();
    }

    public void next(JsonParser.Event expected) {
        JsonParser.Event event;

        event = next();
        if (event != expected) {
            throw new IllegalStateException(event + " vs " + expected);
        }
    }

    public JsonParser.Event eatTimestampsOpt() {
        JsonParser.Event result;

        result = parser.next();
        if (result != JsonParser.Event.KEY_NAME) {
            return result;
        }
        if (!"mdTimestamps".equals(parser.getString())) {
            throw new IllegalStateException();
        }
        next(JsonParser.Event.START_OBJECT);
        next(JsonParser.Event.KEY_NAME);
        next(JsonParser.Event.VALUE_STRING);
        next(JsonParser.Event.END_OBJECT);
        return parser.next();
    }

    public void eatKey(String key) {
        next(JsonParser.Event.KEY_NAME);
        if (!key.equals(parser.getString())) {
            throw new IllegalStateException();
        }
    }

    public String eatKeyValueString(String key) {
        eatKey(key);
        next(JsonParser.Event.VALUE_STRING);
        return parser.getString();
    }

    public long eatKeyValueNumber(String key) {
        eatKey(key);
        next(JsonParser.Event.VALUE_NUMBER);
        return parser.getLong();
    }

    public void eatKeyValueFalse(String key) {
        eatKey(key);
        next(JsonParser.Event.VALUE_FALSE);
    }

    @Override
    public void close() throws Exception {
        parser.close();
    }
}
