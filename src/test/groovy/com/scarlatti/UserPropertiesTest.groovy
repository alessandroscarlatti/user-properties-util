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
class UserPropertiesTest {

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
        UserProperties props = new UserProperties()
        props.load(properties())
        assert props.size() == 4
    }

    @Test
    void "load properties from string via constructor"() {
        UserProperties props = new UserProperties(properties())
        assert props.size() == 4
    }

    @Test
    void "load properties from file via constructor"() {
        UserProperties props = new UserProperties(file)
        assert props.size() == 4
    }

    @Test
    void "save properties to file"() {
        UserProperties props1 = new UserProperties(properties())
        props1.setProperty("key", "getValue")

        assert props1.size() == 5
        props1.store(file)
        UserProperties props2 = new UserProperties(file)
        assert props2.size() == 5

        assert props1 == props2
    }

    @Test
    void "serialize secret properties"() {
        UserProperties props1 = new UserProperties(properties())
                .def("prop1", "the first", true)

        props1.store(file)

        Properties props2 = new UserProperties(file)
        assert props2.getProperty("prop1") != null
        assert props2.getProperty("prop1") != props1.getProperty("prop1")

        Properties props3 = new UserProperties(file, {
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
    void "prompt for properties"() {
        UserProperties props1 = new UserProperties(file, {
            it.def("prop1", "the first", false)
            it.def("prop2", "the second", false)
            it.def("prop3", "the password", true)
        })

        println "done"
    }
}
