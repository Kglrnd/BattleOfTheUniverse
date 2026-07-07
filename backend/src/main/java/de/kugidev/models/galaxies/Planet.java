package de.kugidev.models;

public class Planet {
    private String name;
    private double size;

    public Planet(String name, double size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public double getSize() {
        return size;
    }
}
