module fabric.gen.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp3;
    requires okio;
    requires com.google.gson;
    requires org.jetbrains.annotations;
    requires kotlin.stdlib;
    requires org.apache.commons.lang3;

    opens com.awakenedredstone to javafx.fxml;
    exports com.awakenedredstone;
}