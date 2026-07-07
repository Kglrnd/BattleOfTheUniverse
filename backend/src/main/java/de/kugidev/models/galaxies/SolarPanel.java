package de.kugidev.models;

public class SolarPanel {
    private double efficiency;
    private double capacity;

    public SolarPanel(double efficiency, double capacity) {
        this.efficiency = efficiency;
        this.capacity = capacity;
    }

    public double getEfficiency() {
        return efficiency;
    }

    public double getCapacity() {
        return capacity;
    }

}
