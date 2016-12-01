/**
 * Warlight AI Game Bot
 * 
 * This is an agent made as a submission to the Warlight AI Challenge II by
 * Joshua Dunster, Phillip Little, and Jacob Murphy as part of an undergraduate
 * Introduction to AI course at Clemson University. It was developed using the
 * provided Java program shell created by Jim van Eeden to communicate with the
 * competition program, with our team adding implementations for the function shells
 * provided by that version. The MIT license is included as his shell was released
 * under that license.
 * 
 * @author Joshua Dunster, Phillip Little, Jacob Murphy * 
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package move;
import map.Region;

/**
 * This Move is used in the first part of each round. It represents what Region is increased
 * with how many armies.
 */

public class PlaceArmiesMove extends Move {
	
	private Region region;
	private int armies;
	
	public PlaceArmiesMove(String playerName, Region region, int armies)
	{
		super.setPlayerName(playerName);
		this.region = region;
		this.armies = armies;
	}
	
	/**
	 * @param n Sets the number of armies this move will place on a Region
	 */
	public void setArmies(int n) {
		armies = n;
	}
	
	/**
	 * @return The Region this Move will be placing armies on
	 */
	public Region getRegion() {
		return region;
	}
	
	/**
	 * @return The number of armies this move will place
	 */
	public int getArmies() {
		return armies;
	}
	
	/**
	 * @return A string representation of this Move
	 */
	public String getString() {
		if(getIllegalMove().equals(""))
			return getPlayerName() + " place_armies " + region.getId() + " " + armies;
		else
			return getPlayerName() + " illegal_move " + getIllegalMove();
				
	}
	
}
