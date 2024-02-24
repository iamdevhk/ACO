import java.io.*;

public class Main {
    // Constants for ant colony optimization
    public static final int MAX_ITERATIONS = 1000;
    public static final int NUM_ANTS = 4;
    public static final int NUM_CITIES = 37;
    public static final double PHEROMONE_WEIGHT = 0.5;
    public static final double DISTANCE_WEIGHT = 0.8;
    public static final double PHEROMONE_CONSTANT = 1000;
    public static final double EVAPORATION_RATE = 0.6;
    public static final int MAX_PHEROMONE = 2;
    public static final int START_CITY = 0; // Source city

    public static void main(String[] args) {
        // Create an instance of Ant Colony Optimization
        ACO antColony = new ACO(NUM_ANTS, NUM_CITIES,
                PHEROMONE_WEIGHT, DISTANCE_WEIGHT, PHEROMONE_CONSTANT, 
                EVAPORATION_RATE, MAX_PHEROMONE, START_CITY);

        // Initialize the ant colony
        antColony.init();

        // Read city information from a file
        try 
        {
            File file = new File("cities.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            String lineData;
            int cityIndex = 0;

            // Process each lineData in the file to set city positions and connections
            while ((lineData = bufferedReader.readLine()) != null) {
                String[] cityInfo = lineData.split("\t");
                antColony.setCityPosition(cityIndex, Integer.parseInt(cityInfo[1]), Integer.parseInt(cityInfo[2]));

                // Join each city to every other city (excluding itself)
                for (int otherCityIndex = 0; otherCityIndex < NUM_CITIES; otherCityIndex++) {
                    if (cityIndex == otherCityIndex) continue;
                    antColony.joinCity(cityIndex, otherCityIndex);
                }

                cityIndex++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Record the start time for optimization
        long startTime = System.currentTimeMillis();

        // Perform ant colony optimization
        antColony.optimize(MAX_ITERATIONS);

        // Record the end time after optimization
        long endTime = System.currentTimeMillis();

        // Print the optimization results
        antColony.result();

        // Print the elapsed time for the optimization process
        System.out.println("Elapsed time: " + (endTime - startTime) + "ms");
    }
}
