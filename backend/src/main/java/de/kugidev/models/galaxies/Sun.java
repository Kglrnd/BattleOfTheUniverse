package de.kugidev.models;

public class Sun {
    private String name;
    private double temperature;

    public Sun(String name, double temperature) {
        this.name = name;
        this.temperature = temperature;
    }

    public String getName() {
        return name;
    }

    public double getTemperature() {
        return temperature;
    }
}