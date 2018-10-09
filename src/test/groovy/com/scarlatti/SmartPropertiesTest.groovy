package com.scarlatti

import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Friday, 10/5/2018
 */
class SmartPropertiesTest {

    File file

    @Before
    void setup() {
        Files.createDirectories(Paths.get("build/sandbox"))
        file = new File("build/sandbox/test.properties")
        file.text = ""
    }

    @Test
    void "load properties from string"() {
        SmartProperties props = new SmartProperties()
        props.load(properties())
        assert props.size() == 4
    }

    @Test
    void "load properties from file"() {
        SmartProperties props = new SmartProperties()
        file.text = properties()
        props.load(file)
        assert props.size() == 4
    }

    @Test
    void "save properties to file"() {
        SmartProperties props1 = new SmartProperties()
        props1.load(properties())
        props1.setProperty("key", "getValue")

        assert props1.size() == 5
        props1.store(file)
        SmartProperties props2 = new SmartProperties()
        props2.load(file)
        assert props2.size() == 5

        assert props1 == props2
    }

    @Test
    @Ignore("we don't currently quite support this via the API.")
    void "serialize secret properties"() {
        SmartProperties props1 = new SmartProperties()
        props1.load(properties())

        // .def("prop1", "the first", true)
        props1.store(file)

        Properties props2 = new SmartProperties()
        props2.load(file)
        assert props2.getProperty("prop1") != null
        assert props2.getProperty("prop1") != props1.getProperty("prop1")

        Properties props3 = SmartProperties.get()
            .secretProperty("prop1", "the first")
            .fromFile(file)

        assert props3.getProperty("prop1") != null
        assert props3.getProperty("prop1") == props1.getProperty("prop1")
    }

    static String properties() {
        return """
            prop1=what
            prop2=do
            prop3=you
            prop4=know
        """.stripIndent().trim()
    }

    @Test
    void "prompt for properties when missing a property in the file"() {

        file.text = properties()

        SmartProperties props1 = SmartProperties.get()
            .property("prop1", "the first", false)
            .property("prop2", "the second", false)
            .property("prop3", "the password", true)
            .property("prop4", "the password2", true)
            .property("prop5", "the password3", true)
            .fromFile(file)

        println "done"
        props1.store(file)
    }

    @Test
    void "prompt for properties when file is empty"() {
        SmartProperties props1 = SmartProperties.get()
            .property("prop1", "the first", false)
            .property("prop2", "the second", false)
            .property("prop3", "the password", true)
            .property("prop4", "the password2", true)
            .property("prop5", "the password3", true)
            .fromFile(file)

        println "done"
//        props1.store(file)

        // todo this saving should probably be done automatically!
        // That way, we're just using the properties.
        // We assume they are persisted.
        // Of course we can change them manually afterward
        // and persist those changes as well.
    }

    @Test
    void "use properties builder"() {
        file.text = properties()
        Properties properties = SmartProperties.get()
            .property("prop1", "a very very very very very very long description very very very long description")
            .property("prop2", "the short")
            .property("prop3", "the remarkable property")
            .property("prop5", "the remarkable property")
            .property("prop6", "password for the ftp server (this is your long password) and it ")
            .property("prop7", "the remarkable property")
            .property("prop8", "the remarkable property")
            .property("prop9", "the remarkable property")
            .property("prop0", "the remarkable property")
            .property("propq", "the remarkable property")
            .property("prope", "an example json string for example: qwerlkj asdfjkqwe radfkj qwer asdf")
            .property("propr", "the remarkable property")
            .property("propt", "the remarkable property")
            .property("propy", "the remarkable property")
            .property("propu", "the remarkable property")
            .property("propi", "the remarkable property")
            .secretProperty("com.scarlatti.password", "the password")
            .fromFile(file)

        properties.prop1 != null
    }

    @Test
    void "get one property"() {
        file.text = properties()
        Properties properties = SmartProperties.get()
                .secretProperty("sys.test.password", "your password")
                .fromFile(file)

        properties.prop1 != null
    }

    @Test
    void "get one property when missing file"() {
        file.delete()
        Properties properties = SmartProperties.get()
                .secretProperty("sys.test.password", "your password")
                .fromFile(file)

        properties.prop1 != null
    }

    @Test
    void "get two properties"() {
        file.text = properties()
        Properties properties = SmartProperties.get()
                .property("sys.test.username", "your username")
                .secretProperty("sys.test.password", "your password")
                .fromFile(file)

        properties.prop1 != null
        println properties
    }
}
