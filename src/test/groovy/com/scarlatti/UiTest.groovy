package com.scarlatti

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel
import org.junit.Before
import org.junit.Test

import javax.swing.*

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Saturday, 10/6/2018
 */
class UiTest {

    @Before
    void setup() {
        UIManager.setLookAndFeel(new WindowsLookAndFeel())
    }

    @Test
    void testRenderJPanel() {

        JPanel jPanel = new JPanel()
        JButton jButton = new JButton("what do you know")
        JButton jButton2 = new JButton("what do you know 2")
        jPanel.add(jButton)
        jPanel.add(jButton2)

        JOptionPane.showMessageDialog(null, jPanel)
    }

    @Test
    void buildSwTable() {
        new UserProperties.SwTable({
            it.tr(new UserProperties.Tr({
                it.td(new UserProperties.Td({
                    new JLabel("stuff1")
                }))
                it.td(new UserProperties.Td({
                    new JLabel("stuff2")
                }))
                it.td(new UserProperties.Td({
                    new JLabel("stuff3")
                }))
            }))
        })
    }
}
