package com.awakenedredstone.fabrigen.util;

import javafx.scene.control.ComboBox;

public class LoadableComboBox<T> extends ComboBox<T> {
    private boolean loaded = false;

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded() {
        this.loaded = true;
    }

    public void unsetLoaded() {
        this.loaded = false;
    }
}
