import java.util.*;
import java.io.*;
import scala.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.broadcast.*;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.SparkConf;
import java.lang.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.log;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.Function;

/**
 * This is an Ant Colony Optimization (ACO) program that utilizes the Spark library. 
 * The Traveling Salesman Problem algorithm is the implementation for our algorithm.
 * 
 */

public class ACOSpark {

    public static final int TOTAL_ITERATIONS = 1000; // The amount of times to run the program
    public static final int TOTAL_ANTS = 4; // The amount of Ants per iteration
    public static final int TOTAL_CITIES = 37; // The amount cities for an Ant to travel to

    public static final double ALPHA = 0.5; // Controls the importance of the pheromone trail
    public static final double BETA  = 0.8; // Controls the importance of heuristics (shorter trail)
    public static final double PHEROMONE_CONSTANT     = 1000; // Pheromone evaporation constant
    public static final double MAX_PHEROMONE    = 0.2; // Initial pheromone level
    public static final int    MAX_INITIAL_PHEROMONE = 2; // Pheromone intensity
    public static final int    INITIAL_CITY = 0; // Source city
    private static Randoms randoms; // Used for generating random numners
    public static double [][] cityCoordinates; // Contains a list of each city and its X and Y coordinate
    public static double [][] pheromones; // Holds a list of the pheromone level on the path between two cities
    public static double [][] deltaPheromones; // Maintains the changes in pheromone levels of each path
    public static int [][] graph; // Represents the graph to be traversed by each Ant
    public static int [][] ROUTES; // Holds a list of each Ant's route
    public static int[] bestRoutes; // Keeps track of the order of cities in the route with the shortest distance
    public static double bestDistance; // An Ant's best distance
  
    static class Ant implements Serializable {
        int[] routes; // Maintains the order of the cities an Ant has taken
        double distance; // Keeps track of the distance an Ant has travelled
        int antNumber; // The ith Ant to traverse the algorithm

        public Ant(int antNumber){
            this.routes = new int[TOTAL_CITIES];
            this.distance = 0.0;
            this.antNumber = antNumber;
        };
    }

    /**
     * Sets the X and Y coordinate of a city
     * 
     * @param city a single city
     * @param x the X coordinate of the city
     * @param y the Y coordinate of the city
     */

    public static void setCityPosition(int city, double x, double y) {
        cityCoordinates[city][0] = x;
        cityCoordinates[city][1] = y;
    }

    /**
     * Calculates the Euclidean distance between two cities. 
     * 
     * @param cityi the first city
     * @param cityj the second city
     * @param cities a matrix of cities and their distances
     * 
     */

    private static double distance(int cityi, int cityj, double[][] cities) {
	 return Math.sqrt(Math.pow(cities[cityi][0] - cities[cityj][0], 2)
                + Math.pow(cities[cityi][1] - cities[cityj][1], 2));
    }

    /**
     * Calculates the total length of the ant's route
     * 
     * @param route an array of nodes in the route
     * @param cities a matrix of cities and distances
     * @return a double representing the length of the route 
     * 
     */

    private static double length(int route[], double[][] cities) {
        double sum = 0.0;
        for (int j = 0; j < TOTAL_CITIES; j++) {
            if (j == TOTAL_CITIES - 1) {
                sum += distance(route[j], route[0], cities);
            } else {
                sum += distance(route[j], route[j + 1], cities);
            }
        }
        return sum;
    }

    /**
     * Establishes a bidirectional connection between two cities, denoted by 1
     * Additionally, sets the pheromone level for the path between the cities
     * 
     * @param cityi the first connected city 
     * @param cityj the second connected city
     * 
     */

    public static void connectCities(int cityi, int cityj) {
        graph[cityi][cityj] = 1;
        pheromones[cityi][cityj] = randoms.generateUniform() * MAX_INITIAL_PHEROMONE; 
	    graph[cityj][cityi] = 1;
        pheromones[cityj][cityi] = pheromones[cityi][cityj];
    }

    /**
     * Checks to see if an Ant has visited a city or not
     * 
     * @param nums the cities visited so far
     * @param c the city in question
     * @return true if the city has been visited by the Ant, else false
     * 
     */

    public static boolean visited(int[] nums, int c){
        for(int i = 0; i < nums.length; i++){
            if(nums[i] == c){
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the pheromone deposition by each Ant and updates the pheromone trail 
     * based on each Ant's path that it traveled 
     * Subsequently calculates the pheromone evaporation and updates the pheromone trail
     * 
     * @param currentLength the current length of all the Ant's paths
     * 
     */

    private static void updatePhermoneValues(double[] currentLength) {
        for (int k = 0; k < TOTAL_ANTS; k++) {
            double rlength = currentLength[k]; // current path length for antk
            for (int r = 0; r < TOTAL_CITIES - 1; r++) {
                int cityi = ROUTES[k][r];
                int cityj = ROUTES[k][r + 1];
                deltaPheromones[cityi][cityj] += PHEROMONE_CONSTANT / rlength;
                deltaPheromones[cityj][cityi] += PHEROMONE_CONSTANT / rlength;
            }
        }
        for (int i = 0; i < TOTAL_CITIES; i++) {
            for (int j = 0; j < TOTAL_CITIES; j++) {
                pheromones[i][j] = (1 - MAX_PHEROMONE) * pheromones[i][j] + deltaPheromones[i][j];
                deltaPheromones[i][j] = 0.0;
            }
        }
    }

    /**
     * This is the main entry point of the ACO program.
     * Execution begins here.
     * 
     */
    public static void main(String[] args) {
        List<Ant> temp = new ArrayList<Ant>();

        // Start up Spark
        SparkConf conf = new SparkConf().setAppName("Ant Colony TSP");
        JavaSparkContext jsc = new JavaSparkContext(conf);

        // Create a list of ants
	    for (int j = 0; j < TOTAL_ANTS; j++) {
            temp.add(new Ant(j));
        }

        // Parallelize all Ants to form an RDD
        JavaRDD <Ant> network = jsc.parallelize(temp);

        // Start the timer
        long startTime = System.currentTimeMillis();

        // Instantiate objects
        randoms = new Randoms(21);
        graph = new int[TOTAL_CITIES][];
        cityCoordinates = new double[TOTAL_CITIES][];
        pheromones = new double[TOTAL_CITIES][];
        deltaPheromones = new double[TOTAL_CITIES][];

        // Set default values
        for (int i = 0; i < TOTAL_CITIES; i++) {
            graph[i] = new int[TOTAL_CITIES];
            pheromones[i] = new double[TOTAL_CITIES];
            deltaPheromones[i] = new double[TOTAL_CITIES];
            cityCoordinates[i] = new double[2];

            for (int j = 0; j < 2; j++) {
                cityCoordinates[i][j] = -1.0;
            }

            for (int j = 0; j < TOTAL_CITIES; j++) {
                graph[i][j] = 0;
                pheromones[i][j] = 0.0;
                deltaPheromones[i][j] = 0.0;
            }
        }

        // Initialize Ant routes
        ROUTES = new int[TOTAL_CITIES][];
        for (int i = 0; i < TOTAL_ANTS; i++) {
            ROUTES[i] = new int[TOTAL_CITIES];

            for (int j = 0; j < TOTAL_CITIES; j++) {
                ROUTES[i][j] = -1;
            }
        }

        //  Read in the city coordinates from the given file
        try {
            File file = new File("cities.txt");
            BufferedReader bf = new BufferedReader(new FileReader(file));
            String line;
            int x = 0;

            while ((line = bf.readLine()) != null) {
                String[] words = line.split("\t");
                System.out.println(words);
                setCityPosition(x, Integer.parseInt(words[1]), Integer.parseInt(words[2]));

                for (int j = 0; j < TOTAL_CITIES; j++ ) {
                    if (x == j) continue;
                    connectCities(x, j);
                }
                x++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }        

        // Declare JavaPairRDD for distances to Ants
	    JavaPairRDD<java.lang.Double, Ant> pnetwork = null;
        int iterations = 0;

        // Main ACO loop
        while (iterations < TOTAL_ITERATIONS) { 
            iterations++;

            // Initialize Spark Broadcast variables to hold the lists of city coordinates and pheromone levels on each path
            Broadcast<double[][]> broadcastVar1 = jsc.broadcast(cityCoordinates);    
            Broadcast<double[][]> broadcastVar = jsc.broadcast(pheromones);

            // Take the Ants and perform the ACO algorithm
            // Will return a new RDD of distances and Ants
		    pnetwork = network.mapToPair(new PairFunction<Ant, java.lang.Double, Ant>() { 
                @Override
                public Tuple2 <java.lang.Double, Ant> call(Ant v) {
                    int[] route = new int[TOTAL_CITIES];
                    double alpha = 0.5;
                    double beta = 0.8;   
                    Randoms randoms = new Randoms(21); 
                    Ant result = v;
                    double [][] PROBS = new double[37][];

                    for (int i = 0; i < TOTAL_CITIES; i++) {
                        PROBS[i] = new double[2];
                        for (int j = 0; j < 2; j++) {
                            PROBS[i][j] = -1.0;
                        }
                    }

                    // Calculate the route traveled by the Ant
                    route[0] = 0;
                    for (int i = 0; i < TOTAL_CITIES - 1; i++) {
                        int cityi = route[i];
                        int count = 0;

                        // Calculate the next city to visit
                        for (int c = 0; c < TOTAL_CITIES; c++) {
                            if (cityi == c) {
                                continue;
                            }

                            // Calculate the probability of visiting the next city based on distance and pheromone level
                            // Utilizes the Spark Broadcast to retrieve the distances and pheromone levels
                            if (!visited(route, c)) {
                                double ETAij = Math.pow(1 / distance(cityi, c, broadcastVar1.value()), beta);
                                double TAUij = Math.pow((broadcastVar.value())[cityi][c], alpha);

                                // Calculate the sum of probabilities for visiing the unvisited neighboring cities
                                double sum = 0.0;
                                for (int d = 0; d < TOTAL_CITIES; d++) {
                                    if (!visited(route, d)) {
                                        double ETA = Math.pow(1 / distance(cityi, d, broadcastVar1.value()), beta);
                                        double TAU = Math.pow((broadcastVar.value())[cityi][d], alpha);
                                        sum += ETA * TAU;
                                    }
                                }
                                
                                // Store the calculated probability and city
                                PROBS[count][0] = (ETAij * TAUij) / sum;
                                PROBS[count][1] = (double) c;
                                count++;
                            }
                        }

                        // Use random number generation and get the next city visited
                        double xi = randoms.generateUniform();
                        int y = 0;
                        double sum = PROBS[y][0];
                        while (sum < xi) {
                            y++;
                            sum += PROBS[y][0];
                        }                      
                        route[i + 1] = (int) PROBS[y][1]; // Add the next city to the route
                    }

                    // Return the distance and the Ant back to pnetwork
                    result.routes = route;
                    result.antNumber = v.antNumber;
                    result.distance = length(route, broadcastVar1.value());
                    return new Tuple2(result.distance, result);      
                }
            });

            // Sort by distance and update the best routes and distance
            pnetwork = pnetwork.sortByKey();
            System.out.println("distance is " + pnetwork.collect().get(0)._2().distance);
            if (pnetwork.collect().get(10)._2().distance < bestDistance) {
                bestRoutes = pnetwork.collect().get(0)._2().routes;
                bestDistance = pnetwork.collect().get(0)._2().distance;
            }

            // Update pheromones based on all Ant route lengths
            double[] allLengths = new double[TOTAL_ANTS];
            for (int i = 0; i < TOTAL_ANTS; i++ ) {
                for (int j = 0; j < TOTAL_ANTS; j++) {
                    if (pnetwork.collect().get(j)._2().antNumber == i) {
                        allLengths[i] = pnetwork.collect().get(j)._2().distance;
                        ROUTES[i] = pnetwork.collect().get(j)._2().routes;
                    }
                }
            }

            updatePhermoneValues(allLengths);

            // Reset the network for the next iteration
            pnetwork = pnetwork.mapValues(new Function<Ant, Ant>() {
                @Override
                public Ant call(Ant value) {
                    for(int i = 0; i < TOTAL_CITIES; i++) {
                        value.routes[i] = -1;
                    }
                    value.distance = 0.0;

                    return value;	
		        }            
            });    
        }

        // Stop the timer
        long stopTime = System.currentTimeMillis();
        long totalTime = stopTime - startTime;

        // Display results
        System.out.println("Best distance is " + bestDistance);
        System.out.println("Time taken is " + totalTime + " ms");
        jsc.close(); // Stop the SparkContext
    }
}

