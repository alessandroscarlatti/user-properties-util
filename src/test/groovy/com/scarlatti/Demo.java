package com.scarlatti;

import java.nio.file.Paths;
import java.util.Properties;

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Tuesday, 10/9/2018
 */
public class Demo {

    public static void main(String[] args) {
        Properties props = SmartProperties.get()
            .property("dev.username", "Your Username")
            .secretProperty("dev.password", "Your Super Secret Password")
            .fromFile(Paths.get("build/dev.properties"));

        System.out.println("Username is " + props.getProperty("dev.username"));
        System.out.println("Password is " + props.getProperty("dev.password"));
    }
}
