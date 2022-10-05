package com.awakenedredstone.fabrigen;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import org.apache.commons.io.output.TeeOutputStream;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Wrapper {
    public static final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    public static JFrame startingPopup;

    public static void main(String[] args) {
        TeeOutputStream teeStream = new TeeOutputStream(outputStream, System.out);
        PrintStream out = new PrintStream(teeStream, true);
        System.setOut(out);
        System.setErr(out);
        try {
            startingPopup = startingPopup();
            JavaFX.init(args);
        } catch (Exception e) {
            JFrame frame = new JFrame();
            JPanel panel = new JPanel();

            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setBackground(frame.getBackground());
            textArea.setSize(new Dimension(400, 300));
            textArea.setMaximumSize(new Dimension(400, 300));
            textArea.setMinimumSize(new Dimension(400, 300));

            e.printStackTrace();
            ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
            int n = in.available();
            byte[] bytes = new byte[n];
            in.read(bytes, 0, n);
            String s = new String(bytes, StandardCharsets.UTF_8);
            textArea.setText(s);

            panel.add(textArea);

            frame.setMinimumSize(new Dimension(560, 360));
            frame.setSize(new Dimension(560, 360));
            frame.setTitle("FabriGen console");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            frame.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    textArea.setSize(new Dimension(frame.getWidth() - 60, frame.getHeight() - 60));
                }
            });

            frame.add(panel);
            frame.setVisible(true);
        }
    }

    private static JFrame startingPopup() {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(frame.getBackground());
        textArea.setSize(new Dimension(60, 200));

        textArea.setText("\nStarting FabriGen");

        panel.add(textArea);

        frame.setSize(new Dimension(200, 60));
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);

        frame.add(panel);
        try {
            frame.setIconImage(ImageIO.read(Wrapper.class.getResourceAsStream("icon.png")));
        } catch (IOException ignored) {};
        frame.setVisible(true);
        return frame;
    }
}
