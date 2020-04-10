package com.pkb;

import io.vertx.core.Launcher;

public class Bootstrap {
    public static void main(String[] args) {
        Launcher.executeCommand("run", FdVerticle.class.getCanonicalName());
    }
}
