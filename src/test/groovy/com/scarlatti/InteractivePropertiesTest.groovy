package com.scarlatti

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import org.junit.Before
import org.junit.Test

import javax.swing.UIManager
import java.nio.file.Files
import java.nio.file.Paths

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Friday, 10/5/2018
 */
class InteractivePropertiesTest {

    File file

    @Before
    void setup() {
        Files.createDirectories(Paths.get("build/sandbox"))
        file = new File("build/sandbox/test.properties")
        file.text = ""
        UIManager.setLookAndFeel(new WindowsLookAndFeel())
    }

    @Test
    void "load properties from string"() {
        InteractiveProperties props = new InteractiveProperties()
        props.load(properties())
        assert props.size() == 4
    }

    @Test
    void "load properties from string via constructor"() {
        InteractiveProperties props = new InteractiveProperties(properties())
        assert props.size() == 4
    }

    @Test
    void "load properties from file via constructor"() {
        InteractiveProperties props = new InteractiveProperties(file)
        assert props.size() == 4
    }

    @Test
    void "save properties to file"() {
        InteractiveProperties props1 = new InteractiveProperties(properties())
        props1.setProperty("key", "getValue")

        assert props1.size() == 5
        props1.store(file)
        InteractiveProperties props2 = new InteractiveProperties(file)
        assert props2.size() == 5

        assert props1 == props2
    }

    @Test
    void "serialize secret properties"() {
        InteractiveProperties props1 = new InteractiveProperties(properties())
                .def("prop1", "the first", true)

        props1.store(file)

        Properties props2 = new InteractiveProperties(file)
        assert props2.getProperty("prop1") != null
        assert props2.getProperty("prop1") != props1.getProperty("prop1")

        Properties props3 = new InteractiveProperties(file, {
            it.def("prop1", "the first", true)
        })

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

        InteractiveProperties props1 = new InteractiveProperties(file, {
            it.def("prop1", "the first", false)
            it.def("prop2", "the second", false)
            it.def("prop3", "the password", true)
            it.def("prop4", "the password2", true)
            it.def("prop5", "the password3", true)
        })

        println "done"
        props1.store(file)
    }

    @Test
    void "prompt for properties when file is empty"() {
        InteractiveProperties props1 = new InteractiveProperties(file, {
            it.def("prop1", "the first", false)
            it.def("prop2", "the second", false)
            it.def("prop3", "the password", true)
            it.def("prop4", "the password2", true)
            it.def("prop5", "the password3", true)
        })

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
        Properties properties = InteractiveProperties.get()
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
    void "get one properties"() {
        file.text = properties()
        Properties properties = InteractiveProperties.get()
                .secretProperty("sys.test.password", "your password")
                .fromFile(file)

        properties.prop1 != null
    }

    @Test
    void "get two properties"() {
        file.text = properties()
        Properties properties = InteractiveProperties.get()
                .property("sys.test.username", "your username")
                .secretProperty("sys.test.password", "your password")
                .fromFile(file)

        properties.prop1 != null
    }
}
