package net.oneandone.pommes.scm;

import java.io.IOException;

public class ScmUrlException extends IOException {
    public ScmUrlException(String input, String reason) {
        super(reason + ": " + input);
    }
}
