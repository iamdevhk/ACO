package edu.uwb.css534;
import edu.uw.bothell.css.dsl.MASS.*; 
import java.util.*;

public class Worker extends Agent {
	
	public static final int goElsewhere_ = 0;
	
	public Worker() {
		super();
	}
	public Worker(Object object) {
		super();
	}
	public Object callMethod(int funcId) {
		switch (funcId) {
			case goElsewhere_:
				return goElsewhere();
		}
		return null;
	}
	public Object goElsewhere() {
		int destX = 0; 
		int destY = 0; 
		int min = 1; 
		int currX = getPlace().getIndex()[0], currY = getPlace().getIndex()[1];
		int sizeX = getPlace().getSize()[0], sizeY = getPlace().getSize()[1];
		Random generator = new Random();
		boolean candidatePicked = false;		
		int next = 0;
		next = generator.nextInt(1);
		if (next == 1) {
			destX = currX + generator.nextInt(sizeX - currX - 1);
		} else {
			destX = currX - generator.nextInt(currX);
		}
		
		next = generator.nextInt(1);
		if (next == 1) {
			destY = currY + generator.nextInt(sizeY - sizeY - 1);
		} else {
			destY = currY - generator.nextInt(currY);
		}
		
		migrate(destX, destY);
		return null;
	}
}
