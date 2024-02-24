public class ACO {

    // Parameters for the Ant Colony Optimization algorithm
    private int NUM_ANTS;
    private int START_CITY;
    private int NUM_CITIES;
    private double PHEROMONE_WEIGHT, PHEROMONE_CONSTANT ,DISTANCE_WEIGHT, MAX_PHEROMONE, EVAPORATION_RATE;
    private double shortestLength;
    private Randoms randoms;

    // Data structures for the algorithm
    int[] shortestPath; 
    int [][] antRoutes; 
    int [][] antRouteGraph; 
    double [][] antPheromones;
    double [][] deltaPheromones;
    double [][] cityLeftToTraverse;
    double [][] cities; 

    // Constructor to initialize the ACO parameters
    public ACO(int numberOfAnts, int numberOfCities, double pheromone_weight, double distance_weight, double pheromone_constant, double evaporation_rate, double max_pheromone, int start_city) 
    {
        this.NUM_ANTS = numberOfAnts;
        this.NUM_CITIES = numberOfCities;
        this.PHEROMONE_WEIGHT = pheromone_weight;
        this.DISTANCE_WEIGHT = pheromone_weight;
        this.PHEROMONE_CONSTANT = pheromone_constant;
        this.EVAPORATION_RATE = evaporation_rate;
        this.MAX_PHEROMONE = max_pheromone;
        this.START_CITY = start_city;
        this.randoms = new Randoms(21);
    }

    /**
     * Initializes the matrices and data structures used in the algorithm.
     */
    public void init() 
    {
        this.antRouteGraph = new int[NUM_CITIES][];
        this.cities = new double[NUM_CITIES][];
        this.antPheromones = new double[NUM_CITIES][];
        this.deltaPheromones = new double[NUM_CITIES][];
        this.cityLeftToTraverse = new double[NUM_CITIES][];
        
        for (int firstCity = 0; firstCity < NUM_CITIES; firstCity++) 
        {
            this.antRouteGraph[firstCity] = new int[NUM_CITIES];
            this.cities[firstCity] = new double[2];
            this.antPheromones[firstCity] = new double[NUM_CITIES];
            this.deltaPheromones[firstCity] = new double[NUM_CITIES];
            this.cityLeftToTraverse[firstCity] = new double[2];
            
            for (int secondCity = 0; secondCity < 2; secondCity++) 
            {
                cities[firstCity][secondCity] = -1.0;
                cityLeftToTraverse[firstCity][secondCity] = -1.0;
            }
            
            for (int secondCity = 0; secondCity < NUM_CITIES; secondCity++) 
            {
                antRouteGraph[firstCity][secondCity] = 0;
                antPheromones[firstCity][secondCity] = 0.0;
                deltaPheromones[firstCity][secondCity] = 0.0;
            }
        }

        antRoutes = new int[NUM_CITIES][];
        
        for (int firstCity = 0; firstCity < NUM_ANTS; firstCity++) 
        {
            antRoutes[firstCity] = new int[NUM_CITIES];
            
            for (int secondCity = 0; secondCity < NUM_CITIES; secondCity++) 
            {
                antRoutes[firstCity][secondCity] = -1;
            }
        }

        shortestLength = (double) Integer.MAX_VALUE;
        shortestPath = new int[NUM_CITIES];
        
        for (int cityItr = 0; cityItr < NUM_CITIES; cityItr++) 
        {
            shortestPath[cityItr] = -1;
        }
    }

    /**
     * Joins two cities and initializes the pheromones between them.
     */
    public void joinCity(int firstCity, int secondCity) {
        this.antRouteGraph[firstCity][secondCity] = 1;
        this.antPheromones[firstCity][secondCity] = randoms.generateUniform() * MAX_PHEROMONE; 
        this.antRouteGraph[secondCity][firstCity] = 1;
        this.antPheromones[secondCity][firstCity] = this.antPheromones[firstCity][secondCity];
    }

    /**
     * Sets the position of a city in the 2D space.
     */
    public void setCityPosition(int city, double x_coord, double y_coord) {
        this.cities[city][0] = x_coord;
        this.cities[city][1] = y_coord;
    }

    /**
     * Prints the best route and its length.
     */
    public void result() 
    {
        shortestLength += calcDistance(shortestPath[NUM_CITIES - 1], START_CITY);
        System.out.println(" BEST ROUTE:");
        
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
        {
            if (shortestPath[cityIteration] == 0) 
            {
                System.out.println("source ");
                continue;
            }
            if (shortestPath[cityIteration] >=1 && shortestPath[cityIteration] <= 26) 
            {
                System.out.print((char) (shortestPath[cityIteration] - 1 +'A'));
            } 
            else 
            {
                System.out.print((shortestPath[cityIteration] - 27));
            }
        }
        System.out.println("\n" + "length: " + shortestLength);
    }

    /**
     * Optimizes the routes for a given number of Iterations.
     */
    public void optimize(int ITERATIONS) 
    {
        for (int index = 1; index <= ITERATIONS; index++) 
        {
            for (int antIteration = 0; antIteration < NUM_ANTS; antIteration++) 
            {
                while (0 != isRouteValid(antIteration, index)) 
                {
                    for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
                    {
                        antRoutes[antIteration][cityIteration] = -1;
                    }
                    route(antIteration);
                }

                double pathLength = length(antIteration);

                if (pathLength < shortestLength) 
                {
                    shortestLength = pathLength;
                    for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
                    {
                        shortestPath[cityIteration] = antRoutes[antIteration][cityIteration];
                    }
                }
            }

            updatePHEROMONES();
            for (int antIteration = 0; antIteration < NUM_ANTS; antIteration++) 
            {
                for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
                {
                    antRoutes[antIteration][cityIteration] = -1;
                }
            }
        }
    }

    /**
     * Calculates the Euclidean distance between two cities.
     */
    private double calcDistance(int firstCity, int secondCity) 
    {
        return Math.sqrt(Math.pow(cities[firstCity][0] - cities[secondCity][0], 2)
                + Math.pow(cities[firstCity][1] - cities[secondCity][1], 2));
    }

    /**
     * Checks if a connection exists between two cities in the antRouteGraph.
     */
    private boolean connectionExists(int firstCity, int secondCity) 
    {
        return antRouteGraph[firstCity][secondCity] == 1;
    }

    /**
     * Checks if a specific city has been visited by the ant with the given index.
     */
    private boolean visited(int antIndex, int city) 
    {
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
        {
            if (antRoutes[antIndex][cityIteration] == -1) 
            {
                break;
            }
            if (antRoutes[antIndex][cityIteration] == city) 
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the probability of moving from the firstCity to the secondCity for a given ant.
     */
    private double calculateProbability(int firstCity, int secondCity, int antIndex) 
    {
        double attractive = Math.pow(1 / calcDistance(firstCity, secondCity), DISTANCE_WEIGHT);
        double pheromoneLevel = Math.pow(antPheromones[firstCity][secondCity], PHEROMONE_WEIGHT);

        double sum = 0.0;
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
        {
            if (connectionExists(firstCity, cityIteration)) 
            {
                if (!visited(antIndex, cityIteration)) 
                {
                    double attractiveness = Math.pow(1 / calcDistance(firstCity, cityIteration), DISTANCE_WEIGHT);
                    double pheromone_level = Math.pow(antPheromones[firstCity][cityIteration], PHEROMONE_WEIGHT);
                    sum += attractiveness * pheromone_level;
                }
            }
        }
        return (attractive * pheromoneLevel) / sum;
    }

    /**
     * Calculates the total length of the route for a given ant.
     */
    private double length(int antIndex) 
    {
        double sum = 0.0;
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
        {
            if (cityIteration == NUM_CITIES - 1) 
            {
                sum += calcDistance(antRoutes[antIndex][cityIteration], antRoutes[antIndex][0]);
            } 
            else 
            {
                sum += calcDistance(antRoutes[antIndex][cityIteration], antRoutes[antIndex][cityIteration + 1]);
            }
        }
        return sum;
    }

    /**
     * Chooses the next city for the ant based on probabilities.
     */
    private int city() 
    {
        double randomGen = randoms.generateUniform();
        int cityIndex = 0;
        double sum = cityLeftToTraverse[cityIndex][0];
        while (sum < randomGen) 
        {
            cityIndex++;
            sum += cityLeftToTraverse[cityIndex][0];
        }
        return (int) cityLeftToTraverse[cityIndex][1];
    }

    /**
     * This method defines the route for an ant based on the Ant Colony Optimization algorithm.
     */
    private void route(int antIndex) 
    {
        antRoutes[antIndex][0] = START_CITY;
        for (int cityIteration = 0; cityIteration < NUM_CITIES - 1; cityIteration++) 
        {
            int firstCity = antRoutes[antIndex][cityIteration];
            int count = 0;
            for (int cityIndex = 0; cityIndex < NUM_CITIES; cityIndex++) 
            {
                if (firstCity == cityIndex) 
                {
                    continue;
                }
                if (connectionExists(firstCity, cityIndex)) 
                {
                    if (!visited(antIndex, cityIndex)) 
                    {
                        cityLeftToTraverse[count][0] = calculateProbability(firstCity, cityIndex, antIndex);
                        cityLeftToTraverse[count][1] = (double) cityIndex;
                        count++;
                    }

                }
            }
            // deadlock
            if (0 == count) 
            {
                return;
            }
            antRoutes[antIndex][cityIteration + 1] = city();
        }
    }

    /**
     * check if the route discovered by ant is valid or not
     */
    private int isRouteValid(int antIndex, int iteration) 
    {
        for (int cityIteration = 0; cityIteration < NUM_CITIES - 1; cityIteration++) 
        {
            int firstCity = antRoutes[antIndex][cityIteration];
            int secondCity = antRoutes[antIndex][cityIteration + 1];
            if (firstCity < 0 || secondCity < 0) 
            {
                return -1;
            }
            if (!connectionExists(firstCity, secondCity)) 
            {
                return -2;
            }
            for (int index = 0; index < cityIteration - 1; index++) 
            {
                if (antRoutes[antIndex][cityIteration] == antRoutes[antIndex][index]) 
                {
                    return -3;
                }
            }
        }

        if (!connectionExists(START_CITY, antRoutes[antIndex][NUM_CITIES - 1])) 
        {
            return -4;
        }

        return 0;
    }

    /**
     * Updates the pheromone levels on the edges based on ant routes.
     */
    private void updatePHEROMONES() 
    {
        for (int antInteration = 0; antInteration < NUM_ANTS; antInteration++) 
        {
            double pathLength = length(antInteration); // current path length for antIndex
            for (int routeIteration = 0; routeIteration < NUM_CITIES - 1; routeIteration++) 
            {
                int firstCity = antRoutes[antInteration][routeIteration];
                int secondCity = antRoutes[antInteration][routeIteration + 1];
                deltaPheromones[firstCity][secondCity] += PHEROMONE_CONSTANT / pathLength;
                deltaPheromones[secondCity][firstCity] += PHEROMONE_CONSTANT / pathLength;
            }
        }
        for (int cityIteration = 0; cityIteration < NUM_CITIES; cityIteration++) 
        {
            for (int index = 0; index < NUM_CITIES; index++) 
            {
                antPheromones[cityIteration][index] = (1 - EVAPORATION_RATE) * antPheromones[cityIteration][index] + deltaPheromones[cityIteration][index];
                deltaPheromones[cityIteration][index] = 0.0;
            }
        }
    }
}
