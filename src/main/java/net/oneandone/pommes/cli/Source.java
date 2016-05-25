package net.oneandone.pommes.cli;

import java.util.ArrayList;
import java.util.List;

public class Source {
    public final String url;
    public final List<String> excludes;

    public Source(String url) {
        this.url = url;
        this.excludes = new ArrayList<>();
    }
}
