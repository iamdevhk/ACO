import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.log;

/**
 * Randoms class provides methods for generating random numbers using different distributions.
 */
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
        
        /**
         * Generates a random number from a uniform distribution within the specified range.
         * @param m Range of the uniform distribution (from -m to m).
         * @return Random number from the uniform distribution within the specified range.
         */
        public double sorted(int m) {
            return ThreadLocalRandom.current().nextDouble(-m, m + 1);
        }
    }

