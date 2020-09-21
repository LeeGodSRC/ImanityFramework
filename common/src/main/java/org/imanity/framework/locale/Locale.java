package org.imanity.framework.locale;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.imanity.framework.util.Utility;

import java.util.Map;

public class Locale {

    private final Char2ObjectOpenHashMap<Map<String, String>> translateEntries = new Char2ObjectOpenHashMap<>();

    @Getter
    private final String name;

    protected Locale(String name) {
        this.name = name;
    }

    public void registerEntry(String key, String value) {
        char c = this.getEntry(key);

        Map<String, String> subEntries;
        if (this.translateEntries.containsKey(c)) {
            subEntries = this.translateEntries.get(c);
        } else {
            subEntries = new Object2ObjectOpenHashMap<>();
            this.translateEntries.put(c, subEntries);
        }

        subEntries.put(key, value);
    }

    public void registerEntry(String key, Iterable<String> strings) {
        this.registerEntry(key, Utility.joinToString(strings, "\n"));
    }

    public void registerEntry(String key, String[] strings) {
        this.registerEntry(key, Utility.joinToString(strings, "\n"));
    }

    public void unregisterEntry(String key) {
        char c = this.getEntry(key);

        if (translateEntries.containsKey(c)) {
            Map<String, String> subEntries = this.translateEntries.get(c);

            subEntries.remove(key);

            if (subEntries.isEmpty()) {
                translateEntries.remove(c);
            }
        }
    }

    public String get(String key) {
        char c = this.getEntry(key);

        if (this.translateEntries.containsKey(c)) {
            Map<String, String> subEntries = this.translateEntries.get(c);

            if (subEntries.containsKey(key)) {
                return subEntries.get(key);
            }
            return key;
        }

        return key;
    }

    public char getEntry(String key) {
        return key.charAt(0);
    }
}
