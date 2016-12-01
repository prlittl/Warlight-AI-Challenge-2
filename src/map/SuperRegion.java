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

package map;
import java.util.LinkedList;

public class SuperRegion {
	
	private int id;
	private int armiesReward;
	private LinkedList<Region> subRegions;
	
	public SuperRegion(int id, int armiesReward)
	{
		this.id = id;
		this.armiesReward = armiesReward;
		subRegions = new LinkedList<Region>();
	}
	
	public void addSubRegion(Region subRegion)
	{
		if(!subRegions.contains(subRegion))
			subRegions.add(subRegion);
	}
	
	/**
	 * @return A string with the name of the player that fully owns this SuperRegion
	 */
	public String ownedByPlayer()
	{
		String playerName = subRegions.getFirst().getPlayerName();
		for(Region region : subRegions)
		{
			if (!playerName.equals(region.getPlayerName()))
				return null;
		}
		return playerName;
	}
	
	/**
	 * @return The id of this SuperRegion
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * @return The number of armies a Player is rewarded when he fully owns this SuperRegion
	 */
	public int getArmiesReward() {
		return armiesReward;
	}
	
	/**
	 * @return A list with the Regions that are part of this SuperRegion
	 */
	public LinkedList<Region> getSubRegions() {
		return subRegions;
	}
}
