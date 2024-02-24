import mpi.*;
import java.io.*;

public class MainMpi {

    // Constants for ant colony optimization
    private static ACOMpi antColony;
    public static final int MAX_ITERATIONS = 3000;
    public static final int NUM_ANTS = 8;
    public static final int NUM_CITIES = 37;
    public static final int MAX_PHEROMONE = 2;
    public static final int START_CITY = 0; // Source city
    public static final double PHEROMONE_WEIGHT = 0.5;
    public static final double DISTANCE_WEIGHT = 1.0;
    public static final double PHEROMONE_CONSTANT = 1000;
    public static final double EVAPORATION_RATE = 0.5;


    public static void main(String[] args) throws MPIException {
        // Initialize MPI
        MPI.Init(args);
        
        // Get the rank and number of processes
        int rank = MPI.COMM_WORLD.Rank();
        int numberOfProcesses = MPI.COMM_WORLD.Size();

        // Create an instance of ACOMpi for the ant colony optimization
        antColony = new ACOMpi(NUM_ANTS, NUM_CITIES, PHEROMONE_WEIGHT, DISTANCE_WEIGHT,
                                PHEROMONE_CONSTANT, EVAPORATION_RATE, 
                                    MAX_PHEROMONE, START_CITY, rank, numberOfProcesses);

        // Initialize the ant colony
        antColony.init();

        try {
            // Read city information from a file
            java.io.File file = new File("cities.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            String lineData;
            int cityIndex = 0;

            // Process each lineData in the file to set city positions and connections
            while ((lineData = bufferedReader.readLine()) != null) 
            {
                String[] cityInfo = lineData.split("\t");
                antColony.setCityPosition(cityIndex, Integer.parseInt(cityInfo[1]), 
                                            Integer.parseInt(cityInfo[2]));

                // Connect each city to every other city (excluding itself)
                for (int otherCityIndex = 0; otherCityIndex < NUM_CITIES; otherCityIndex++) {
                    if (cityIndex == otherCityIndex) continue;
                    antColony.connectCities(cityIndex, otherCityIndex);
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

        // Print results only from process with rank 0
        if (rank == 0) 
        {
            antColony.result();
            long endTime = System.currentTimeMillis();
            System.out.println("Elapsed time: " + (endTime - startTime) + "ms");
        }

        // Finalize MPI
        MPI.Finalize();
    }
}
