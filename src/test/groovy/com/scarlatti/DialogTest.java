package com.scarlatti;

import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

/**
 * ______    __                         __           ____             __     __  __  _
 * ___/ _ | / /__ ___ ___ ___ ____  ___/ /______    / __/______ _____/ /__ _/ /_/ /_(_)
 * __/ __ |/ / -_|_-<(_-</ _ `/ _ \/ _  / __/ _ \  _\ \/ __/ _ `/ __/ / _ `/ __/ __/ /
 * /_/ |_/_/\__/___/___/\_,_/_//_/\_,_/_/  \___/ /___/\__/\_,_/_/ /_/\_,_/\__/\__/_/
 * Monday, 10/8/2018
 */
public class DialogTest {

    @Before
    public void setup() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

    }

    @Test
    public void testInternalDialog() throws Exception {
        JFrame frame = new JFrame("Test Dialog");
        JPanel jPanel = new JPanel();

        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        int response = JOptionPane.showInternalOptionDialog(
            jPanel,
            new JTextField("hey what do you know"),
            "Edit Properties",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"OK", "Cancel"},
            "OK"
        );

        CountDownLatch latch = new CountDownLatch(1);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error awaiting window close.", e);
        }
    }

    @Test
    public void testJFrameDialog() {
        JFrame frame = new JFrame("My dialog asks....");

        frame.setUndecorated( true );
        frame.setVisible( true );
        frame.setLocationRelativeTo( null );

        String message = JOptionPane.showInputDialog(
            frame,
            "Would this be enough?.",
            "My dialog asks....",
            JOptionPane.INFORMATION_MESSAGE);

        System.out.println( "Got " + message );

        frame.dispose();
    }
}
