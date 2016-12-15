package net.oneandone.pommes.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PomTest {
    @Test
    public void json() {
        JsonObject obj;
        Pom pom;

        obj = new JsonParser().parse(
                "  {\n" +
                "    \"id\": \"zone:origin\",\n" +
                "    \"revision\": \"32\",\n" +
                "    \"parent\": \"net.oneandone.maven.poms:lazy-foss-parent:1.0.1\",\n" +
                "    \"artifact\": \"net.oneandone:pommes:3.0.2-SNAPSHOT\",\n" +
                "    \"scm\": \"git:ssh://git@github.com/mlhartme/pommes.git\",\n" +
                "    \"url\": \"https://github.com/mlhartme/pommes\",\n" +
                "    \"dependencies\": [\n" +
                "        \"net.oneandone:sushi:3.1.1\"\n" +
                "    ]\n" +
                "  }").getAsJsonObject();
        pom = Pom.fromJson(obj);
        assertEquals("zone:origin", pom.id);
        assertEquals("32", pom.revision);
        assertEquals(obj, pom.toJson());
    }
}
