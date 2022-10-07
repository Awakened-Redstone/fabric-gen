package com.awakenedredstone.fabrigen;

import org.apache.commons.io.output.TeeOutputStream;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Wrapper {
    public static final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    public static JFrame startingPopup;

    public static void main(String[] args) {
        TeeOutputStream outStream = new TeeOutputStream(outputStream, System.out);
        TeeOutputStream errStream = new TeeOutputStream(outputStream, System.err);
        PrintStream out = new PrintStream(outStream, true);
        PrintStream err = new PrintStream(errStream, true);
        System.setOut(out);
        System.setErr(err);
        startingPopup = startingPopup();
        try {
            System.out.println("============= Ignore the JavaFX warning. =============");
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

            try {
                frame.setIconImage(ImageIO.read(Wrapper.class.getResourceAsStream("icon.png")));
            } catch (Exception ignored) {};

            frame.setMinimumSize(new Dimension(560, 360));
            frame.setSize(new Dimension(560, 360));
            frame.setTitle("FabriGen logs");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            frame.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    textArea.setSize(new Dimension(frame.getWidth() - 60, frame.getHeight() - 60));
                }
            });

            frame.add(panel);
            frame.setVisible(true);
            startingPopup.setVisible(false);
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
        } catch (Exception ignored) {};
        frame.setVisible(true);
        return frame;
    }
}
