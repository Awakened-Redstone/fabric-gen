module fabric.gen.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires okio;
    requires com.google.gson;
    requires org.jetbrains.annotations;
    requires kotlin.stdlib;
    requires org.apache.commons.lang3;
    requires java.xml;
    requires velocity;
    requires velocity.tools;
    requires commons.collections;
    requires commons.beanutils;
    requires struts.core;
    requires struts.tiles;
    requires struts.taglib;
    requires sslext;

    opens com.awakenedredstone to javafx.fxml;
    exports com.awakenedredstone;
}