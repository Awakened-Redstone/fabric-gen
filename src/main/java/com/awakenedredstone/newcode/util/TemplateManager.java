package com.awakenedredstone.newcode.util;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.StringWriter;
import java.util.Map;

public class TemplateManager {
    public static String generateTemplate(Map<String, String> values, String template) {
        VelocityContext context = new VelocityContext();
        final StringWriter stringWriter = new StringWriter();
        values.forEach(context::put);

        Velocity.evaluate(context, stringWriter, "Velocity", template);

        return stringWriter.toString();
    }
}
