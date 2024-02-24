package edu.uwb.css534;

import java.io.IOException;
import java.io.Serializable;

public class DataPOJO implements Serializable {
    private static final long serialVersionUID = 7526472295622776147L;
    private int[] cities;
    private double coordinates[];

    public static DataPOJO build(int city, int x_coord, int y_coord){
        return new DataPOJO().setCities(new int[]{city, x_coord, y_coord});
    }
    public int[] getCities() {
        return cities;
    }

    public DataPOJO setCities(int[] cities) {
        this.cities = cities;
        return this;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}
