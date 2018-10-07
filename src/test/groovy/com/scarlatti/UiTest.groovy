package com.scarlatti

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
        SmartProperties.SwTable table = new SmartProperties.SwTable({
            it.tr(new SmartProperties.Tr("1", {
                it.td(new SmartProperties.Td(
                    new SmartProperties.SwLabel("name")
                ))
                it.td(new SmartProperties.Td(
                    new SmartProperties.SwTextField("value")
                ))
                it.td(new SmartProperties.Td(
                    new SmartProperties.SwLabel("description")
                ))
            }))
        })

        JOptionPane.showMessageDialog(null, table.render())

        println "value is now: " + table.getTrs().get(0).getTds().get(1).getUi().getValue()
    }
}
