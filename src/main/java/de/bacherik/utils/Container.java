package de.bacherik.utils;

public class Container {

    private final String content;

    public Container(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public int getLength() {
        return content.length();
    }
}
