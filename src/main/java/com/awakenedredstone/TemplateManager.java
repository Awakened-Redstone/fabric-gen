package com.awakenedredstone;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.util.StringUtils;

import java.io.StringWriter;
import java.util.Map;

public class TemplateManager {
    private VelocityContext context;

    public static TemplateManager create() {
        return new TemplateManager();
    }

    public void assertRequired() {
        assert context != null;
    }

    public void createVelocityContext() {
        context = new VelocityContext();
        context.put("StringUtils", StringUtils.class);
    }

    public String generateTemplate(Map<String, String> values, String template) {
        final StringWriter stringWriter = new StringWriter();
        values.forEach((key, value) -> context.put(key, value));

        Velocity.evaluate(context, stringWriter, "Velocity", template);

        return stringWriter.toString();
    }
}
