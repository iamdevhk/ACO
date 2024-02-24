import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.log;

public class Main {
	
	// Driver method for the MapReduce job
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		JobConf conf = new JobConf(Main.class);
		conf.setJobName("AntColonyOptimization_TSP");
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);
		conf.setMapperClass(Map.class);
		conf.setReducerClass(Reduce.class);
		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		JobClient.runJob(conf);
		System.out.println("Elapsed time = " + (System.currentTimeMillis() - time) + " ms");
	}
	
	// Mapper class for the MapReduce job
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
		
		public static final double PHEROMONE_CONSTANT = 1000;
		public static final double MAX_PHEROMONE = 0.5;
		public static final double EVAPORATION_RATE = 0.5;
		public static final double ATTRACTIVENESS = 0.8;
		public static final int MAX_INITIAL_PHEROMONE = 2;
		public static final int INITIAL_CITY = 0;
		final int TOTAL_CITIES = 37;
		final int TOTAL_ITERATIONS = 1000;
		final int TOTAL_ANTS = 4;
		
		JobConf conf;
		int[][] cityCoordinates = new int[TOTAL_CITIES][2];
		
		public void configure(JobConf job) { // Method to configure the Mapper
			this.conf = job;
		}
		
		public void map(LongWritable key, Text value, OutputCollector<Text, Text> output,
		                Reporter reporter) throws
				IOException {
			AntColony ants = new AntColony(TOTAL_ANTS, TOTAL_CITIES, EVAPORATION_RATE, ATTRACTIVENESS, PHEROMONE_CONSTANT, MAX_PHEROMONE, MAX_INITIAL_PHEROMONE, INITIAL_CITY);
			ants.init();
			
			String line = value.toString();
			StringTokenizer tokenizer = new StringTokenizer(line);
			int citiesProcessed = 0;
			// Create graph
			while (tokenizer.hasMoreTokens()) {
				tokenizer.nextToken();
				cityCoordinates[citiesProcessed][0] = Integer.parseInt(tokenizer.nextToken());
				cityCoordinates[citiesProcessed][1] = Integer.parseInt(tokenizer.nextToken());
				citiesProcessed++;
			}
			// calculate calculateDistanceBetweenCities between each cities
			ants.calculateDistanceMatrix(cityCoordinates);
			ants.optimize(TOTAL_ITERATIONS);
			String result = ants.printResults();
			output.collect(new Text("Result"), new Text(result));
		}
		
		public class AntColony {
			
			int[] bestRoute; // Represents the best route in the current iteration
			int[][] antRoutes; // Stores the routes of all ants from 0 to ANTSIZE-1
			double[][] cityCoordinates;// CITIES[i][j] stands for the calculateDistanceBetweenCities between city i and city j
			double[][] pheromones; // Represents the pheromones on every edge
			double[][] deltaPheromones; // Stores the changes in pheromone levels
			double[][] probabilities; // Stores the probabilities for ant movement
			double[][] distanceMatrix; // Represents the calculateDistanceBetweenCities between every pair of cities
			private Randoms randoms;
			private int TOTAL_ANTS, TOTAL_CITIES, INITIAL_CITY;
			private double EVAPORATION_RATE, ATTRACTIVENESS, PHEROMONE_CONSTANT, MAX_PHEROMONE, MAX_INITIAL_PHEROMONE;
			private double optimalRouteLength;
			
			public AntColony(int totAnts, int totCities, double evaRate, double attractiveness, double ph, double maxPh,
			           double initPh, int initCity) {
				this.TOTAL_ANTS = totAnts;
				this.TOTAL_CITIES = totCities;
				this.EVAPORATION_RATE = evaRate;
				this.ATTRACTIVENESS = attractiveness;
				this.PHEROMONE_CONSTANT = ph;
				this.MAX_PHEROMONE = maxPh;
				this.MAX_INITIAL_PHEROMONE = initPh;
				this.INITIAL_CITY = initCity;
				this.randoms = new Randoms(21);
			}
					
			public void init() { // Method to initialize the Ant Colony
				this.cityCoordinates = new double[TOTAL_CITIES][2];
				this.pheromones = new double[TOTAL_CITIES][TOTAL_CITIES];
				this.deltaPheromones = new double[TOTAL_CITIES][TOTAL_CITIES];
				this.probabilities = new double[TOTAL_CITIES][2];		
				this.distanceMatrix = new double[TOTAL_CITIES][TOTAL_CITIES];
				
				antRoutes = new int[TOTAL_ANTS][TOTAL_CITIES];
				bestRoute = new int[TOTAL_CITIES];
				for (int row = 0; row < TOTAL_CITIES; row++) {
					bestRoute[row] = -1;
					
					for (int col = 0; col < 2; col++) {
						probabilities[row][col] = -1.0;
					}
					
					for (int col = 0; col < TOTAL_CITIES; col++) {
						initial_Pheromones(row, col);
					}
				}
				
				for (int row = 0; row < TOTAL_ANTS; row++) {
					for (int col = 0; col < TOTAL_CITIES; col++) {
						antRoutes[row][col] = -1;
					}
				}
				
				optimalRouteLength = (double) Integer.MAX_VALUE;
			}
			
			public void optimize(int ITERATIONS) { // Method to optimize the ant routes
				
				for (int iteration = 1; iteration <= ITERATIONS; iteration++) {
					for (int antItr = 0; antItr < TOTAL_ANTS; antItr++) {
						while (0 != checkRouteValidity(antItr, iteration)) {
							for (int itr = 0; itr < TOTAL_CITIES; itr++) {
								antRoutes[antItr][itr] = -1;
							}
							route(antItr);
						}
						double currentRouteLength  = calculateTotalDistance(antItr);
						if (currentRouteLength  < optimalRouteLength) {
							optimalRouteLength = currentRouteLength ;
							for (int itr = 0; itr < TOTAL_CITIES; itr++) {
								bestRoute[itr] = antRoutes[antItr][itr];
							}
						}
						updatePheromones();
					for (int antItr = 0; antItr < TOTAL_ANTS; antItr++) {
						for (int cityItr = 0; cityItr < TOTAL_CITIES; cityItr++) {
							antRoutes[antItr][cityItr] = -1;
						}
					}
				}
			}
			
		
			private void route(int antIndex) { //Determines the route for a given ant based on the current city and available probabilities
				antRoutes[antIndex][0] = INITIAL_CITY;
				for (int currentIndex  = 0; currentIndex  < TOTAL_CITIES - 1; currentIndex ++) {
					int currentCity  = antRoutes[antIndex][currentIndex ];
					int count = 0;
					
					for (int nextCity  = 0; nextCity  < TOTAL_CITIES; nextCity ++) {
						if (currentCity  == nextCity ) {
							continue;
						}
						
						if (!visited(antIndex, nextCity )) { // Check if the next city is not visited by this ant
							probabilities[count][0] = calculateProbability(currentCity , nextCity , antIndex);
							probabilities[count][1] = (double) nextCity ;
							count++;
						}
					}
					
					if (count==0) {
						return;
					}
					antRoutes[antIndex][currentIndex  + 1] = city();
				}
			}
			
			private int city() { //Selects the next city for an ant based on calculated probabilities, it uses a random value and cumulative probabilities to choose the city.
				double randomValue  = randoms.generateUniform();
				int selectedCityIndex = 0;
				double cumulativeProbability  = probabilities[selectedCityIndex][0];
				while (cumulativeProbability  < randomValue ) {
					selectedCityIndex++;
					cumulativeProbability  += probabilities[selectedCityIndex][0];
				}
				return (int) probabilities[selectedCityIndex][1];
			}
			
			private boolean visited(int antIndex, int nextCity ) { //Checks if a city has been visited by a specific ant during its route.
				for (int cityIndex  = 0; cityIndex  < TOTAL_CITIES; cityIndex ++) {  // Iterate through the ant's route to check if the city has been visited
					if (antRoutes[antIndex][cityIndex ] == -1) {
						break;
					}
					if (antRoutes[antIndex][cityIndex ] == nextCity ) {
						return true;
					}
				}
				return false;
			}
			
			//Initializes the pheromone levels between two cities with random values.
			public void initial_Pheromones(int cityIndex1, int cityIndex2) {
				this.pheromones[cityIndex1][cityIndex2] = randoms.generateUniform() * MAX_INITIAL_PHEROMONE; // init random pheromones
				this.pheromones[cityIndex2][cityIndex1] = pheromones[cityIndex1][cityIndex2];
			}
			
			private double calculateDistanceBetweenCities(int cityIndex1, int cityIndex2) { //sCalculates the Euclidean distance between two cities based on their coordinates.
				double xDiff = cityCoordinates[cityIndex1][0] - cityCoordinates[cityIndex2][0];
				double yDiff = cityCoordinates[cityIndex1][1] - cityCoordinates[cityIndex2][1];
			
				return Math.hypot(xDiff, yDiff);
			}
			
			private double getDistanceFromMatrix(int row, int col) { //Retrieves the distance value between two cities from the distance matrix.
				return distanceMatrix[row][col];
			}
			
			
			void calculateDistanceMatrix(int[][] cityCoordinates) { //Calculates the distance matrix between all pairs of cities based on their coordinates
				
				for (int sourceCity = 0; sourceCity < TOTAL_CITIES; ++sourceCity) { // Loop through each pair of cities to calculate distances
					for (int destinationCity = 0; destinationCity < TOTAL_CITIES; ++destinationCity) {
						distanceMatrix[sourceCity][destinationCity] = Math.hypot(
							(cityCoordinates[sourceCity][0] - cityCoordinates[destinationCity][0]),
							(cityCoordinates[sourceCity][1] - cityCoordinates[destinationCity][1])
						);
					}
				}
			}
			
			private double calculateTotalDistance(int antIndex) { // Calculates the total distance traveled by an ant along its route
				double totalDistance = 0.0;
				for (int cityIndex = 0; cityIndex < TOTAL_CITIES; cityIndex++) {
					int currentCity = antRoutes[antIndex][cityIndex];
					int nextCity = (cityIndex == TOTAL_CITIES - 1) ? antRoutes[antIndex][0] : antRoutes[antIndex][cityIndex + 1];
					
					totalDistance += getDistanceFromMatrix(currentCity, nextCity);
				}
				return totalDistance;
			}
			
			
			private double calculateProbability(int currentCity, int nextCity, int antIndex) { //Calculates the probability of an ant moving from the current city to the next city based on attractiveness and pheromone levels
				double attractivenessToNext = Math.pow(1 / getDistanceFromMatrix(currentCity, nextCity), ATTRACTIVENESS);
				double pheromoneLevel = Math.pow(pheromones[currentCity][nextCity], EVAPORATION_RATE);
				
				double sum = 0.0;
				for (int city = 0; city < TOTAL_CITIES; city++) { // Calculate sum of pheromone levels and attractiveness for unvisited cities
					if (!visited(antIndex, city)) {
						double eta = Math.pow(1 / getDistanceFromMatrix(currentCity, city), ATTRACTIVENESS);
						double tau = Math.pow(pheromones[currentCity][city], EVAPORATION_RATE);
						sum += eta * tau;
					}
				}
				return (attractivenessToNext * pheromoneLevel) / sum;     // Calculate and return the probability of moving to the next city
			}
			
			
			
			private int checkRouteValidity(int antIndex, int iteration) { // Checks the validity of the route traversed by an ant in the ant colony optimization process.
				for (int currentIndex = 0; currentIndex < TOTAL_CITIES - 1; currentIndex++) {
					int currentCity = antRoutes[antIndex][currentIndex];
					int nextCity = antRoutes[antIndex][currentIndex + 1];
					
		
					if (currentCity < 0 || nextCity < 0) {
						return -1;
					}
					
				
					for (int previousIndex = 0; previousIndex < currentIndex - 1; previousIndex++) {
						if (antRoutes[antIndex][currentIndex] == antRoutes[antIndex][previousIndex]) {
							return -3; // if the City revisited, invalid route
						}
					}
				}
				return 0;
			}
			
			
			private void updatePheromones() { //Updates the pheromone levels on paths based on ant routes and performs pheromone evaporation
				
				for (int antIndex = 0; antIndex < TOTAL_ANTS; antIndex++) {
					double currentRouteLength = calculateTotalDistance(antIndex); // Calculate the total length of the current ant's route
					for (int routeIndex = 0; routeIndex < TOTAL_CITIES - 1; routeIndex++) {
						int cityFrom = antRoutes[antIndex][routeIndex];
						int cityTo = antRoutes[antIndex][routeIndex + 1];
						// Increment delta pheromones on the path between cityFrom and cityTo
						deltaPheromones[cityFrom][cityTo] += PHEROMONE_CONSTANT / currentRouteLength;
						deltaPheromones[cityTo][cityFrom] += PHEROMONE_CONSTANT / currentRouteLength;
					}
				}
				
				// Update pheromones on all paths with evaporation and addition of delta pheromones
				for (int pheromoneIndexA = 0; pheromoneIndexA < TOTAL_CITIES; pheromoneIndexA++) {
					for (int pheromoneIndexB = 0; pheromoneIndexB < TOTAL_CITIES; pheromoneIndexB++) { 
						 // Evaporate existing pheromones and add the delta pheromones
						pheromones[pheromoneIndexA][pheromoneIndexB] = (1 - EVAPORATION_RATE) * pheromones[pheromoneIndexA][pheromoneIndexB] + deltaPheromones[pheromoneIndexA][pheromoneIndexB];
						deltaPheromones[pheromoneIndexA][pheromoneIndexB] = 0.0;
					}
				}
			}
			
			//helps us in printing the result
			public String printResults() {
				StringBuilder resultStringBuilder = new StringBuilder();
				optimalRouteLength += calculateDistanceBetweenCities(bestRoute[TOTAL_CITIES - 1], INITIAL_CITY); 
			
				resultStringBuilder.append(optimalRouteLength);
				System.out.println("BEST ROUTE: ");
			
				// Generates sequence of all visited cities
				for (int cityIndex = 0; cityIndex < TOTAL_CITIES; cityIndex++) {
					if (bestRoute[cityIndex] == 0) {
						System.out.println("source");
						resultStringBuilder.append("source ");
						continue;
					}
					if (bestRoute[cityIndex] >= 1 && bestRoute[cityIndex] <= 26) {
						resultStringBuilder.append((char) (bestRoute[cityIndex] - 1 + 'A'));
					} else {
						resultStringBuilder.append((bestRoute[cityIndex] - 27));
					}
				}
				
				System.out.println(resultStringBuilder.toString());
				System.out.println("\n" + "length: " + optimalRouteLength);
				return resultStringBuilder.toString();
			}
		}
		
		//class for generating random numbers using different distributions.
		public class Randoms {
			
			public static final int MULTIPLIER  = 16807;
			public static final int MODULUS  = Integer.MAX_VALUE;
			public static final double AM = 1.0 / MODULUS ;
			public static final int QUOTIENT  = 127773;
			public static final int REMAINDER  = 2836;
			public static final int TABLE_SIZE  = 32;
			public static final double INVERSE_DIVISOR  = 1 + (MODULUS  - 1) / TABLE_SIZE ;
			public static final double EPSILON  = 1.2e-7;
			public static final double MAXIMUM_RANDOM  = 1.0 - EPSILON ;
			
			
			long previousValue  = 0; // Variables for maintaining state and generating random numbers
			long shuffleTable [] = new long[TABLE_SIZE ];
			int isetFlag  = 0;
			
			double storedRandom;
			private long[] xpto;
			
			private Random randomGenerator;
			// Constructor initializing the state variables
			public Randoms(long x) {
				this.xpto = new long[2];
				this.xpto[0] = -x;
				randomGenerator = new Random();
			}
			
			public double generateNormal(double average, double sigma) { //Generates a random number from a normal (Gaussian) distribution.
				return average + sigma * generateGaussian(this.xpto);
			}
			
			public double generateUniform() { //Generates a random number from a uniform distribution
				return random1(this.xpto);
			}
			
			
			public double random1(long[] idum) { //Generates a random number from a uniform distribution 
				int j;
				long k;
				
				double temp;
				if (idum[0] <= 0 || previousValue  == 0) {          
					if (-(idum[0]) < 1) {
						idum[0] = 1;    
					} else { idum[0] = -(idum[0]); }
					for (
							j = TABLE_SIZE  + 7;
							j >= 0; j--) {      
						k = (idum[0]) / QUOTIENT ;
						idum[0] = MULTIPLIER  * (idum[0] - k * QUOTIENT ) - REMAINDER  * k;
						if (idum[0] < 0) { idum[0] += MODULUS ; }
						if (j < TABLE_SIZE ) { shuffleTable [j] = idum[0]; }
					}
					previousValue  = shuffleTable [0];
				}
				
				k = (idum[0]) / QUOTIENT ;                  
				idum[0] =
						MULTIPLIER  * (idum[0] - k * QUOTIENT ) -
								REMAINDER  * k;      
				if (idum[0] < 0) {
					idum[0] += MODULUS ;       
				}
				j = (int) (previousValue  / INVERSE_DIVISOR );                       
				previousValue  = shuffleTable [j];                        
				shuffleTable [j] = idum[0];              
				if ((temp = AM * previousValue ) > MAXIMUM_RANDOM ) {
					return MAXIMUM_RANDOM ;                  
				} else { return temp; }
			}
			
			public double generateGaussian(long[] idum) { //Generates a random number from a Gaussian distribution
				double factor, squareSum, v1, v2; 
				if (idum[0] < 0) {
					isetFlag  = 0;      
				}
				if (isetFlag  == 0) {           
					do {
						v1 = 2.0 * random1(idum) -
								1.0;    
						v2 = 2.0 * random1(idum) - 1.0;    
						squareSum = v1 * v1 + v2 * v2;         
						
					} while (squareSum >= 1.0 || squareSum == 0.0);  
					factor = Math.sqrt(-2.0 * log(squareSum) / squareSum);
				
					storedRandom = v1 * factor;
					isetFlag  = 1;                 
					return v2 * factor;
				} else {                    
					isetFlag  = 0;                
					return storedRandom;           
				}
			}
			
		}
	}
}
	

	//reducer class that processes intermediate values for a MapReduce job.
	public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
		
		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output,
		                   Reporter reporter) throws IOException {
			System.out.println();
			System.out.println("----REDUCER----");
			System.out.println();
			
			HashMap<Integer, Text> map = new HashMap<>();
			double max = Double.MAX_VALUE;
			String result = "";
			
			while (values.hasNext()) {
				String re = values.next().toString();
				double len = Double.parseDouble(re.substring(0, re.indexOf("source") - 1));
				if (max > len) {
					max = len;
					result = re;
				}
			}
			System.out.println(result);
			output.collect(new Text("Result"), new Text(result));
		}
	}
	
}