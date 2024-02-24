package edu.uwb.css534;
import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;
import java.io.*;
import java.util.Arrays;

public class ACO_Mass {

    private static final String nodes_File = "nodes.xml";

    public static int TOTAL_ITERATIONS = 1000;
    public static int TOTAL_ANTS = 4;
    public static final int TOTAL_CITIES = 37;

    public static final double PHEROMONE_CONSTANT = 1000;
	public static final double MAX_PHEROMONE = 0.5;
	public static final double EVAPORATION_RATE = 0.5;
	public static final double ATTRACTIVENESS = 0.8;
	public static final int MAX_INITIAL_PHEROMONE = 2;
	public static final int INITIAL_CITY = 0;

    public static void main(String[] args) {
        MASS.setNodeFilePath(nodes_File);
        MASS.setLoggingLevel(LogLevel.ERROR);
        
        TOTAL_ITERATIONS = Integer.parseInt(args[1]);
        
        TOTAL_ANTS = Integer.parseInt(args[2]);
        MASS.init();
        long startTimer = System.currentTimeMillis();
        Places places = new Places(1, City.class.getName(), null, TOTAL_CITIES);
        Agents ants = new Agents(1, ACO.class.getName(), null, places, TOTAL_ANTS);

        ants.callAll(ACO.INIT);

        try {

            File file = new File(args[0]);
            BufferedReader bf = new BufferedReader(new FileReader(file));
            String cityLine;
            int cityIndex = 0;
            while ((cityLine = bf.readcityLine()) != null) {
                String[] lineData = cityLine.split("\t");
                DataPOJO data = DataPOJO.build(cityIndex,Integer.parseInt(lineData[1]),Integer.parseInt(lineData[2]));
                
                ants.callAll(ACO.SET_POSITION, data);
                for (int destCityIndex = 0; destCityIndex < TOTAL_CITIES; destCityIndex++ )
                {
                    if (cityIndex == destCityIndex) continue;
                    ants.callAll(ACO.CONNECT, (Object) Arrays.asList(cityIndex,destCityIndex).toArray());
                }
                cityIndex++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }

        for (int itr = 0; itr < TOTAL_ITERATIONS; itr++) {
            ants.callAll(ACO.OPTIMIZE);
        }

        ACO.results();
        
        long endTimer = System.currentTimeMillis();
        
        System.out.println("Elapsed time: "+ (endTimer - startTimer)+" ms");
        
        MASS.finish();

    }
}
