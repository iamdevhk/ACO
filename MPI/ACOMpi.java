import mpi.*;

/**
 * ACOMpi class represents an Ant Colony Optimization algorithm using MPI for parallelization.
 */
public class ACOMpi {
    // Parameters for the Ant Colony Optimization algorithm
    private int NUM_ANTS;
    private int START_CITY;
    private int NUM_CITIES;
    private double PHEROMONE_WEIGHT, PHEROMONE_CONSTANT ,DISTANCE_WEIGHT, MAX_PHEROMONE, EVAPORATION_RATE;
    private double shortestLength;
    private Randoms randoms;
    private int rank;
    private int size;
    private int rankOfset;

    // Data structures for representing the problem and solution
    int[] shortestPath; 
    int[][] antRouteGraph;   
    int[][] antRoutes;  
    double[][] cities; 
    double[] antPheromones; 
    double[][] deltaPheromones;
    double[][] cityLeftToTraverse;

    /**
     * Constructor for ACOMpi class.
     */
    public ACOMpi(int numberOfAnts, int numberOfCities, double pheromone_weight, double distance_weight, double pheromone_constant, double evaporation_rate, double max_pheromone, int start_city, int rank, int size) {
        // Initialization of parameters
        this.NUM_ANTS = numberOfAnts;
        this.NUM_CITIES = numberOfCities;
        this.PHEROMONE_WEIGHT = pheromone_weight;
        this.DISTANCE_WEIGHT = distance_weight;
        this.PHEROMONE_CONSTANT = pheromone_constant;
        this.EVAPORATION_RATE = evaporation_rate;
        this.MAX_PHEROMONE = max_pheromone;
        this.START_CITY = start_city;
        this.randoms = new Randoms(21);
        this.rank = rank;
        this.size = size;
        this.rankOfset = rank * NUM_CITIES * NUM_CITIES;
    }

    /**
     * Initializes the data structures for the ACO algorithm.
     */
    public void init() {
        // Initialization of data structures
        this.antRouteGraph = new int[NUM_CITIES][];
        this.cities = new double[NUM_CITIES][];
        this.antPheromones = new double[size * NUM_CITIES * NUM_CITIES];
        this.deltaPheromones = new double[NUM_CITIES][];
        this.cityLeftToTraverse = new double[NUM_CITIES][];
        for (int index = 0; index < size; index++) {
            for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) {
                this.antRouteGraph[cityIteration] = new int[NUM_CITIES];
                this.cities[cityIteration] = new double[2];
                this.deltaPheromones[cityIteration] = new double[NUM_CITIES];
                this.cityLeftToTraverse[cityIteration] = new double[2];
                for (int cityIndex = 0; cityIndex < 2; cityIndex++) {
                    cities[cityIteration][cityIndex] = -1.0;
                    cityLeftToTraverse[cityIteration][cityIndex] = -1.0;
                }
                for (int cityIndex = 0; cityIndex < NUM_CITIES; cityIndex++) {
                    antRouteGraph[cityIteration][cityIndex] = 0;
                    this.antPheromones[rankOfset + cityIteration * NUM_CITIES + cityIndex] = 0.0;
                    deltaPheromones[cityIteration][cityIndex] = 0.0;
                }
            }
        }

        // Initialization of antRoutes and best route
        antRoutes = new int[NUM_ANTS][];
        for (int antsIteration = 0; antsIteration < NUM_ANTS; antsIteration++) {
            antRoutes[antsIteration] = new int[NUM_CITIES];
            for (int cityItr = 0; cityItr < NUM_CITIES; cityItr++) {
                antRoutes[antsIteration][cityItr] = -1;
            }
        }

        shortestLength = (double) Integer.MAX_VALUE;
        shortestPath = new int[NUM_CITIES];
        for (int cityIndex = 0; cityIndex < NUM_CITIES; cityIndex++) {
            shortestPath[cityIndex] = -1;
        }
    }

    /**
     * Connects two cities in the antRouteGraph matrix and initializes random pheromones.
     */
    public void connectCities(int firstCity, int secondCity) {
        this.antRouteGraph[firstCity][secondCity] = 1;
        this.antPheromones[rankOfset + firstCity * NUM_CITIES + secondCity] = randoms.generateUniform() * MAX_PHEROMONE;
        this.antRouteGraph[secondCity][firstCity] = 1;
        this.antPheromones[rankOfset + secondCity * NUM_CITIES + firstCity] = this.antPheromones[rankOfset + firstCity * NUM_CITIES + secondCity];
    }

    /**
     * Sets the position of a city in the cities matrix.
     */
    public void setCityPosition(int city, double x_coord, double y_coord) {
        this.cities[city][0] = x_coord;
        this.cities[city][1] = y_coord;
    }
    
    /**
     * Print the results of the Ant Colony Optimization algorithm.
     * It prints the best route found and its length.
     */
    public void result() {
        shortestLength += calcDistance(shortestPath[NUM_CITIES - 1], START_CITY);
        System.out.println(" BEST ROUTE:");
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) {
            if (shortestPath[cityIteration] == 0) {
                System.out.println("source ");
                continue;
            }
            if (shortestPath[cityIteration] >= 1 && shortestPath[cityIteration] <= 26) {
                System.out.print((char) (shortestPath[cityIteration] - 1 + 'A'));
            } else {
                System.out.print((shortestPath[cityIteration] - 27));
            }
        }
        System.out.println("\n" + "length: " + shortestLength);
    }

    /**
     * Optimize the Ant Colony Optimization algorithm for a specified number of iterations.
     */
    public void optimize(int ITERATIONS) {
        for (int index = 1; index <= ITERATIONS; index++) {
            for (int antIteration = 0; antIteration < NUM_ANTS; antIteration++) {
                while (0 != isRouteValid(antIteration, index)) {
                    for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) {
                        antRoutes[antIteration][cityIteration] = -1;
                    }
                    route(antIteration);
                }
                double pathLength = length(antIteration);
                if (pathLength < shortestLength) {
                    shortestLength = pathLength;
                    for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) {
                        shortestPath[cityIteration] = antRoutes[antIteration][cityIteration];
                    }
                }
            }
            updatePheromones();
            for (int antItr = 0; antItr < NUM_ANTS; antItr++) {
                for (int cityItr = 0; cityItr < NUM_CITIES; cityItr++) {
                    antRoutes[antItr][cityItr] = -1;
                }
            }
        }
    }

    /**
     * Calculate the Euclidean calcDistance between two cities.
     */
    private double calcDistance(int firstCity, int secondCity) {
        return Math.sqrt(Math.pow(cities[firstCity][0] - cities[secondCity][0], 2)
                + Math.pow(cities[firstCity][1] - cities[secondCity][1], 2));
    }

    /**
     * Check if a connection connectionExists between two cities in the antRouteGraph matrix.
     */
    private boolean connectionExists(int firstCity, int secondCity) {
        return antRouteGraph[firstCity][secondCity] == 1;
    }

    /**
     * Check if a city has been visited by a particular ant.
     */
    private boolean visited(int antIndex, int city) {
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) {
            if (antRoutes[antIndex][cityIteration] == -1) {
                break;
            }
            if (antRoutes[antIndex][cityIteration] == city) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate the Phi value for the transition probability in the Ant Colony Optimization algorithm.
     */
    private double calculateProbability(int firstCity, int secondCity, int antIndex) {
        double attractive = Math.pow(1 / calcDistance(firstCity, secondCity), DISTANCE_WEIGHT);
        double pheromoneLevel = Math.pow(antPheromones[rankOfset + firstCity * NUM_CITIES + secondCity], PHEROMONE_WEIGHT);

        double sum = 0.0;
        for (int cityIndex = 0; cityIndex < NUM_CITIES; cityIndex++) {
            if (connectionExists(firstCity, cityIndex)) {
                if (!visited(antIndex, cityIndex)) {
                    double attractiveness = Math.pow(1 / calcDistance(firstCity, cityIndex), DISTANCE_WEIGHT);
                    double pheromone_level = Math.pow(antPheromones[rankOfset + firstCity * NUM_CITIES + cityIndex], PHEROMONE_WEIGHT);
                    sum += attractiveness * pheromone_level;
                }
            }
        }
        return (attractive * pheromoneLevel) / sum;
    }

    /**
     * Calculate the total length of the route taken by an ant.
     */
    private double length(int antIndex) {
        double sum = 0.0;
        for (int cityItr = 0; cityItr < NUM_CITIES; cityItr++) {
            if (cityItr == NUM_CITIES - 1) {
                sum += calcDistance(antRoutes[antIndex][cityItr], antRoutes[antIndex][0]);
            } else {
                sum += calcDistance(antRoutes[antIndex][cityItr], antRoutes[antIndex][cityItr + 1]);
            }
        }
        return sum;
    }

    /**
     * Choose the next city for the ant based on transition probabilities.
     */
    private int city() {
        double randomGen = randoms.generateUniform();
        int cityIndex = 0;
        double sum = cityLeftToTraverse[cityIndex][0];
        while (sum < randomGen) {
            cityIndex++;
            sum += cityLeftToTraverse[cityIndex][0];
        }
        return (int) cityLeftToTraverse[cityIndex][1];
    }

    /**
     * Generate the route for an ant using the transition probabilities.
     */
    private void route(int antIndex) {
        antRoutes[antIndex][0] = START_CITY;
        for (int cityIteration = 0; cityIteration < NUM_CITIES - 1; cityIteration++) {
            int firstCity = antRoutes[antIndex][cityIteration];
            int count = 0;
            for (int cityIndex = 0; cityIndex < NUM_CITIES; cityIndex++) {
                if (firstCity == cityIndex) {
                    continue;
                }
                if (connectionExists(firstCity, cityIndex)) {
                    if (!visited(antIndex, cityIndex)) {
                        cityLeftToTraverse[count][0] = calculateProbability(firstCity, cityIndex, antIndex);
                        cityLeftToTraverse[count][1] = (double) cityIndex;
                        count++;
                    }
                }
            }

            if (0 == count) {
                return; // deadlock
            }

            antRoutes[antIndex][cityIteration + 1] = city();
        }
    }

    /**
     * check if the route discovered by ant is valid or not
     */
    private int isRouteValid(int antIndex, int iteration) {
        for (int cityItr = 0; cityItr < NUM_CITIES - 1; cityItr++) {
            int firstCity = antRoutes[antIndex][cityItr];
            int secondCity = antRoutes[antIndex][cityItr + 1];
            if (firstCity < 0 || secondCity < 0) {
                return -1;
            }
            if (!connectionExists(firstCity, secondCity)) {
                return -2;
            }
            for (int index = 0; index < cityItr - 1; index++) {
                if (antRoutes[antIndex][cityItr] == antRoutes[antIndex][index]) {
                    return -3;
                }
            }
        }

        if (!connectionExists(START_CITY, antRoutes[antIndex][NUM_CITIES - 1])) {
            return -4;
        }

        return 0; // Route is Valid
    }

    /**
     * Update the pheromone levels on the edges based on the antRoutes taken by the ants.
     */
    private void updatePheromones() {
        for (int antsItr = 0; antsItr < NUM_ANTS; antsItr++) {
            double pathLength = length(antsItr); // current path length for antIndex
            for (int cityItr = 0; cityItr < NUM_CITIES - 1; cityItr++) {
                int firstCity = antRoutes[antsItr][cityItr];
                int secondCity = antRoutes[antsItr][cityItr + 1];
                deltaPheromones[firstCity][secondCity] += PHEROMONE_CONSTANT / pathLength;
                deltaPheromones[secondCity][firstCity] += PHEROMONE_CONSTANT / pathLength;
            }
        }
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) {
            for (int index = 0; index < NUM_CITIES; index++) {
                antPheromones[rankOfset + cityIteration * NUM_CITIES + index] = 
                (1 - EVAPORATION_RATE) * antPheromones[rankOfset + cityIteration * NUM_CITIES + index] 
                + deltaPheromones[cityIteration][index];
                deltaPheromones[cityIteration][index] = 0.0;
            }
        }
    }

    /**
     * Get the MPI rank of the current process.
     */
    public int getRank() {
        return rank;
    }

}