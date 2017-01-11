package RUfoo.managers;

/**
 * Design buildable scalable mapLocations.
 * Format:
 *  x,y origin
 *  t - tree
 *  e - empty space
 * @author Ben
 *
 */
public class BuildInstructions {
	
	public static final String SQUARE = 
			"1,2\n"
			+ "t t t t\n"
			+ "t e e t\n"
			+ "t e e t\n"
			+ "t t t t";
	
	public static final String CHECKER = 
			"2,2\n"
			+ "t e t e\n"
			+ "e e e t\n"
			+ "t e e e\n"
			+ "e t e t";
	
}
