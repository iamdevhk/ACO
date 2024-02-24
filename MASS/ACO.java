package edu.uwb.css534;

import edu.uw.bothell.css.dsl.MASS.Agent;

public class ACO extends Agent {
    public static final int INIT = 0;
    public static final int CONNECT = 1;
    public static final int SET_POSITION = 2;
    public static final int OPTIMIZE = 3;

    public static int[] shortestPath; 
    int [][] antRouteGraph;  
    int [] antRoutes; 
    static double [][] cities; 
    public static double [][] antPheromones;
    double [][] deltaPheromones;
    double [][] cityLeftToTraverse
;

    private static double shortestLength
;
    private Randoms randoms;

    public ACO (Object o) 
    {
        randoms = new Randoms(21);
    }
   
    /*
     * Override the callMethod function of the parent Agent class
     */

    @Override
    public Object callMethod(int id, Object args) {
        switch (id) {
            case INIT: return init();
            case CONNECT: return joinCity( args);
            case SET_POSITION: return setCityPosition(args);
            case OPTIMIZE: return optimize();
            default:
                return "Method ID is not found " + id;
        }
    }

    /*
     * Method to connect two cities in the ant route graph
     */
    public Object joinCity(Object cityInfo) 
    {
        Integer[] cities = (Integer[]) cityInfo;
        int firstCity = cities[0];
        int secondCity = cities[1];
        this.antRouteGraph[firstCity][secondCity] = 1;
        this.antPheromones[firstCity][secondCity] = randoms.generateUniform() * ACO_Mass.MAX_INITIAL_PHEROMONE; 
        this.antRouteGraph[secondCity][firstCity] = 1;
        this.antPheromones[secondCity][firstCity] = this.antPheromones[firstCity][secondCity];
        return null;
    }

    /*
     * Method to set the position of a city in the coordinate system
     */
    public Object setCityPosition(Object cityInfo) 
    {
        DataPOJO dataPOJO = (DataPOJO) cityInfo;
        int city = dataPOJO.getCities()[0];
        double x_coord = dataPOJO.getCities()[1];
        double y_coord = dataPOJO.getCities()[2];
        cities[city][0] = x_coord;
        cities[city][1] = y_coord;
        return null;
    }

    /*
     * Method to initialize the ACO algorithm
     */
    public Object init() 
    {
        
        this.antRouteGraph = new int[ACO_Mass.TOTAL_CITIES][];
        this.cities = new double[ACO_Mass.TOTAL_CITIES][];
        this.antPheromones = new double[ACO_Mass.TOTAL_CITIES][];
        this.deltaPheromones = new double[ACO_Mass.TOTAL_CITIES][];
        this.cityLeftToTraverse = new double[ACO_Mass.TOTAL_CITIES][];
        for (int cityIteration = 0; cityIteration < ACO_Mass.TOTAL_CITIES; cityIteration++) 
        {
            this.antRouteGraph[cityIteration] = new int[ACO_Mass.TOTAL_CITIES];
            this.cities[cityIteration] = new double[2];
            this.antPheromones[cityIteration] = new double[ACO_Mass.TOTAL_CITIES];
            this.deltaPheromones[cityIteration] = new double[ACO_Mass.TOTAL_CITIES];
            this.cityLeftToTraverse[cityIteration] = new double[2];
            for (int index = 0; index < 2; index++) 
            {
                cities[cityIteration][index] = -1.0;
                cityLeftToTraverse[cityIteration][index] = -1.0;
            }
            for (int index = 0; index < ACO_Mass.TOTAL_CITIES; index++) 
            {
                antRouteGraph[cityIteration][index] = 0;
                antPheromones[cityIteration][index] = 0.0;
                deltaPheromones[cityIteration][index] = 0.0;
            }
        }

        antRoutes = new int[ACO_Mass.TOTAL_CITIES];
        for (int index = 0; index < ACO_Mass.TOTAL_CITIES; index++) 
        {
            antRoutes[index] = -1;
        }


        shortestLength = (double) Integer.MAX_VALUE;
        shortestPath = new int[ACO_Mass.TOTAL_CITIES];
        for (int cityIndex = 0; cityIndex < ACO_Mass.TOTAL_CITIES; cityIndex++) 
        {
            shortestPath[cityIndex] = -1;
        }

        return null;
    }

    /*
     * Method to optimize the ant routes
     */
    public Object optimize() 
    {

        while (0 != isRouteValid()) 
        {
            for (int cityItr = 0; cityItr < ACO_Mass.TOTAL_CITIES; cityItr++) 
            {
                antRoutes[cityItr] = -1;
            }
            route();
        }

        double pathLength = length();

        if (pathLength < shortestLength) 
        {
            shortestLength = pathLength;
            for (int index = 0; index < ACO_Mass.TOTAL_CITIES; index++) 
            {
                shortestPath[index] = antRoutes[index];
            }
        }

        updatePHEROMONES();

        for (int cityItr = 0; cityItr < ACO_Mass.TOTAL_CITIES; cityItr++) 
        {
            antRoutes[cityItr] = -1;
        }
        return null;
    }

    /*
     * Method to calculate the length of the ant route
     */
    private double length() {
        double sum = 0.0;
        for (int cityItr = 0; cityItr < ACO_Mass.TOTAL_CITIES; cityItr++) 
        {
            if (cityItr == ACO_Mass.TOTAL_CITIES - 1) {
                sum += calcDistance(antRoutes[cityItr], antRoutes[0]);
            } 
            else 
            {
                sum += calcDistance(antRoutes[cityItr], antRoutes[cityItr + 1]);
            }
        }
        return sum;
    }

    /*
     * Method to determine the next city in the ant route
     */
    private void route() 
    {
        antRoutes[0] = ACO_Mass.INITIAL_CITY;
        for (int cityIteration = 0; cityIteration < ACO_Mass.TOTAL_CITIES - 1; cityIteration++) 
        {
            int cityIndex = antRoutes[cityIteration];
            int count = 0;
            for (int index = 0; index < ACO_Mass.TOTAL_CITIES; index++) 
            {
                if (cityIndex == index) 
                {
                    continue;
                }

                if (!visited(index)) 
                {
                    cityLeftToTraverse[count][0] = calculateProbability(cityIndex,index);
                    cityLeftToTraverse[count][1] = (double) index;
                    count++;
                }
            }
            // deadlock
            if (0 == count) 
            {
                return;
            }

            antRoutes[cityIteration + 1] = city();
        }
    }

    /*
     * Method to check if the current ant route is valid
     */
    private int isRouteValid() 
    {
        for (int cityIndex = 0; cityIndex < ACO_Mass.TOTAL_CITIES - 1; cityIndex++) 
        {
            int firstCity = antRoutes[cityIndex];
            int secondCity = antRoutes[cityIndex + 1];
            if (firstCity < 0 || secondCity < 0) 
            {
                return -1;
            }
            for (int index = 0; index < cityIndex - 1; index++) 
            {
                if (antRoutes[cityIndex] == antRoutes[index]) 
                {
                    return -3;
                }
            }
        }
        return 0;
    }

    /*
     * Method to select the next city in the ant route based on probability
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

    /*
     * Method to calculate the probability of moving from one city to another
     */
    private double calculateProbability(int firstCity, int secondCity) 
    {
        double attractive = Math.pow(1 / calcDistance(firstCity, secondCity), ACO_Mass.ATTRACTIVENESS);
        double pheromoneLevel = Math.pow(antPheromones[firstCity][secondCity], ACO_Mass.MAX_PHEROMONE);
        double sum = 0.0;
        for (int cityIndex = 0; cityIndex < ACO_Mass.TOTAL_CITIES; cityIndex++) 
        {
            if (!visited(cityIndex)) 
            {
                double attractiveness_val = Math.pow(1 / calcDistance(firstCity, cityIndex), ACO_Mass.ATTRACTIVENESS);
                double pheromone_level = Math.pow(antPheromones[firstCity][cityIndex], ACO_Mass.MAX_PHEROMONE);
                sum += attractiveness_val * pheromone_level;
            }

        }
        return (attractive * pheromoneLevel) / sum;
    }

    /*
     * Method to check if a city has been visited in the current ant route
     */
    private boolean visited(int cityIndex) 
    {
        for (int cityItr = 0; cityItr < ACO_Mass.TOTAL_CITIES; cityItr++) 
        {
            if (antRoutes[cityItr] == -1) 
            {
                break;
            }
            if (antRoutes[cityItr] == cityIndex) 
            {
                return true;
            }
        }
        return false;
    }

    /*
     * Method to calculate the Euclidean distance between two cities
     */
    private static double calcDistance(int firstCity, int secondCity) 
    {
        return Math.sqrt(Math.pow(cities[firstCity][0] - cities[secondCity][0], 2) 
                            + Math.pow(cities[firstCity][1] - cities[secondCity][1], 2));
    }

    /*
     * Method to update pheromone levels on ant routes
     */
    private void updatePHEROMONES() 
    {
        double pathLength = length();
        for (int cityIteration = 0; cityIteration < ACO_Mass.TOTAL_CITIES - 1; cityIteration++) 
        {
            int firstCity = antRoutes[cityIteration];
            int secondCity = antRoutes[cityIteration + 1];
            deltaPheromones[firstCity][secondCity] += ACO_Mass.PHEROMONE_CONSTANT / pathLength;
            deltaPheromones[secondCity][firstCity] += ACO_Mass.PHEROMONE_CONSTANT / pathLength;
        }
        for (int cityItr = 0; cityItr < ACO_Mass.TOTAL_CITIES; cityItr++) 
        {
            for (int index = 0; index < ACO_Mass.TOTAL_CITIES; index++) 
            {
                antPheromones[cityItr][index] = (1 - ACO_Mass.EVAPORATION_RATE) * antPheromones[cityItr][index] + deltaPheromones[cityItr][index];
                deltaPheromones[cityItr][index] = 0.0;
            }
        }
    }

    /*
     * Method to print the results, i.e., the best route and its length
     */
    public static void results() 
    {
        shortestLength += calcDistance(shortestPath[ACO_Mass.TOTAL_CITIES - 1], ACO_Mass.INITIAL_CITY);
        System.out.println(" BEST ROUTE:");
        for (int cityIndex = 0; cityIndex < ACO_Mass.TOTAL_CITIES; cityIndex++) 
        {
            if (shortestPath[cityIndex] == 0) 
            {
                System.out.println("source ");
                continue;
            }
            if (shortestPath[cityIndex] >=1 && shortestPath[cityIndex] <= 26) 
            {
                System.out.print((char) (shortestPath[cityIndex] - 1 +'A'));
            } 
            else 
            {
                System.out.print((shortestPath[cityIndex] - 27));
            }
        }
        System.out.println("\n" + "length: " + shortestLength);
    }
}
