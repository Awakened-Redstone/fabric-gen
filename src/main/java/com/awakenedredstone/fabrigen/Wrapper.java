package com.awakenedredstone.fabrigen;

import org.apache.commons.configuration2.plist.SimpleCharStream;
import org.apache.velocity.runtime.parser.CharStream;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Wrapper {
    public static void main(String[] args) {
        try {
            JavaFX.init(args);
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(baos);

            JFrame frame = new JFrame();
            JPanel panel = new JPanel();

            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setBackground(frame.getBackground());
            textArea.setSize(new Dimension(400, 300));
            textArea.setMaximumSize(new Dimension(400, 300));
            textArea.setMinimumSize(new Dimension(400, 300));

            e.printStackTrace(out);
            ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
            int n = in.available();
            byte[] bytes = new byte[n];
            in.read(bytes, 0, n);
            String s = new String(bytes, StandardCharsets.UTF_8);
            textArea.setText(s);

            panel.add(textArea);

            frame.setMinimumSize(new Dimension(560, 360));
            frame.setSize(new Dimension(560, 360));
            frame.setTitle("FabriGen fallback");
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
}
