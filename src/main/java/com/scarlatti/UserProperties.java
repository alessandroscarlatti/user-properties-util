package com.scarlatti;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Friday, 10/5/2018
 */
public class UserProperties extends Properties {

    // property definitions are optional.
    // if we read without definitions we will get the raw values.
    // there would be no options for encoding.
    // as far as reading is concerned, the only thing that matters
    // is actually the secret properties.
    private List<PropertyDef> propertyDefs = new ArrayList<>();

    public UserProperties() {
    }

    public UserProperties(String properties) {
        load(properties);
    }

    public UserProperties(String properties, Consumer<UserProperties> config) {
        config.accept(this);
        load(properties);
    }

    public UserProperties(File file) {
        load(file);
    }

    public UserProperties(File file, Consumer<UserProperties> config) {
        config.accept(this);
        load(file);
    }

    public UserProperties(Properties defaults) {
        super(defaults);
    }

    public UserProperties def(String name, String description, boolean secret) {
        return def(new PropertyDef(name, description, secret));
    }

    public UserProperties def(PropertyDef propertyDef) {
        propertyDefs.add(propertyDef);
        return this;
    }

    public void load(File file) {
        Objects.requireNonNull(file, "File may not be null");
        if (!file.exists())
            throw new IllegalStateException("File " + file + "does not exist.");

        try(FileInputStream fis = new FileInputStream(file)) {
            load(fis);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from file " + file, e);
        }
    }

    public void load(String properties) {
        try(StringReader reader = new StringReader(properties)) {
            load(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from string " + properties, e);
        }
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
        decodeSecretProperties();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
        decodeSecretProperties();
    }

    private void decodeSecretProperties() {
        // if a property with that name exists AND is marked as secret,
        // decode it and change its value.
        for (PropertyDef def : propertyDefs) {
            if (getProperty(def.getName()) != null && def.getSecret()) {
                String decodedValue = new String(getDecoder().decode(getProperty(def.getName())));
                setProperty(def.getName(), decodedValue);
            }
        }
    }

    public void store(File file) {
        store(file, "");
    }

    public void store(File file, String comments) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            store(writer, comments);
        } catch (Exception e) {
            throw new RuntimeException("Error storing properties.", e);
        }
    }

    @Override
    public synchronized void store(Writer writer, String comments) throws IOException {
        encodeSecretProperties();
        super.store(writer, comments);
        decodeSecretProperties();
    }

    @Override
    public synchronized void store(OutputStream out, String comments) throws IOException {
        encodeSecretProperties();
        super.store(out, comments);
        decodeSecretProperties();
    }

    private void encodeSecretProperties() {
        // if a property with that name exists AND is marked as secret,
        // decode it and change its value.
        for (PropertyDef def : propertyDefs) {
            if (getProperty(def.getName()) != null && def.getSecret()) {
                String encodedValue = new String(getEncoder().encode(getProperty(def.getName()).getBytes()));
                setProperty(def.getName(), encodedValue);
            }
        }
    }

    static class PropertyDef {
        private String name;
        private String description;
        private boolean secret;

        public PropertyDef() {
        }

        public PropertyDef(String name, String description, boolean secret) {
            this.name = name;
            this.description = description;
            this.secret = secret;
        }

        public PropertyDef(PropertyDef other) {
            this.name = other.name;
            this.description = other.description;
            this.secret = other.secret;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean getSecret() {
            return secret;
        }

        public void setSecret(boolean secret) {
            this.secret = secret;
        }
    }
}
