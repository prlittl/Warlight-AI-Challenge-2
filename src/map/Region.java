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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;


public class Region {
	
	private int id;
	private LinkedList<Region> neighbors;
	private SuperRegion superRegion;
	private int armies;
	private String playerName;
	
	
	private int armiesWanted = 0;
	
	

	public Region(int id, SuperRegion superRegion)
	{
		this.id = id;
		this.superRegion = superRegion;
		this.neighbors = new LinkedList<Region>();
		this.playerName = "unknown";
		this.armies = 0;
		
		superRegion.addSubRegion(this);
	}
	
	public Region(int id, SuperRegion superRegion, String playerName, int armies)
	{
		this.id = id;
		this.superRegion = superRegion;
		this.neighbors = new LinkedList<Region>();
		this.playerName = playerName;
		this.armies = armies;
		
		superRegion.addSubRegion(this);
	}
	
	//TODO Trace exception
	public void addNeighbor(Region neighbor)
	{
		if(neighbor != null && !neighbors.contains(neighbor)) //added null check to avoid exception. may not solve issue...
		{
			neighbors.add(neighbor);
			neighbor.addNeighbor(this);
		}
	}
	
	/**
	 * @param region a Region object
	 * @return True if this Region is a neighbor of given Region, false otherwise
	 */
	public boolean isNeighbor(Region region)
	{
		if(neighbors.contains(region))
			return true;
		return false;
	}

	/**
	 * @param playerName A string with a player's name
	 * @return True if this region is owned by given playerName, false otherwise
	 */
	public boolean ownedByPlayer(String playerName)
	{
		if(playerName.equals(this.playerName))
			return true;
		return false;
	}
	
	/**
	 * @param armies Sets the number of armies that are on this Region
	 */
	public void setArmies(int armies) {
		this.armies = armies;
	}
	
	/**
	 * @param playerName Sets the Name of the player that this Region belongs to
	 */
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}
	
	/**
	 * @return The id of this Region
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * @return A list of this Region's neighboring Regions
	 */
	public LinkedList<Region> getNeighbors() {
		return neighbors;
	}
	
	/**
	 * @return The SuperRegion this Region is part of
	 */
	public SuperRegion getSuperRegion() {
		return superRegion;
	}
	
	/**
	 * @return The number of armies on this region
	 */
	public int getArmies() {
		return armies;
	}
	
	/**
	 * @return A string with the name of the player that owns this region
	 */
	public String getPlayerName() {
			return playerName;
	}
	
	/**
	 * @return the set value of the armiesWanted.
	 */
	public int getWantedArmies() {
		return armiesWanted;
	}

	/**
	 * @param armiesWanted Sets the heuristic value for this region, 
	 */
	public void setWantedArmies(int armiesWanted) {
		this.armiesWanted = armiesWanted;
	}
	
	/**
	 * @return If this region has the same ID as the passed region.
	 */
	@Override
	public boolean equals(Object o)
	{
		return (this.id == ((Region)(o)).id);
	}
	
	/**
	 * @return True if this region borders territories not owned by the player,
	 * 		   false otherwise
	 */
	public boolean isBorder()
	{
		for(Region adjacent : neighbors)
		{
			//Return true if any one adjacent region is owned by another player
			if(!adjacent.playerName.equals(this.playerName))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return The region adjacent to this region which is closest to a
	 * 		   border region, or null if no such path exists.
	 */
	public Region closestAdjacentToBorder()
	{
		/* If called on a border region, return the region itself 
		 */
		if(this.isBorder())
		{
			return this;
		}
		
		ArrayList<Region> frontier = new ArrayList<Region>();
		/* Map id of child region to id of parent region to recover path
		 * after searching for nearest border region
		 */
		HashMap<Integer, Integer> reverseTree = new HashMap<Integer, Integer>();
		frontier.add(this);
		reverseTree.put(this.id, this.id);
		Region curr;
		/* Current index in frontier so we don't remove elements, as that may cause
		 * the search to go back to already-explored nodes
		 */
		int currNum = 0;
		
		/* Main loop to perform a Breadth-First Search for nearest border region
		 */
		while(currNum != frontier.size())
		{
			curr = frontier.get(currNum);
			for(Region adj : curr.neighbors)
			{
				/* If a border region is found, look for the step which
				 * led to that region from this region
				 */
				if(adj.isBorder())
				{
					/* If we are on the current node, return the adjacent node
					 * without backtracking as it will be the first step
					 */
					if(curr.equals(this))
					{
						return adj;
					}
					
					int currID = curr.id;
					
					/* Backtrack through the trail of region IDs until we get
					 * back to this region
					 */
					while(reverseTree.get(currID) != this.id)
					{
						currID = reverseTree.get(currID);
					}
					
					/* Once at this region, find the neighbor with the ID given
					 * by the next step backtracking
					 */
					for(Region step : this.neighbors)
					{
						if(step.id == currID)
						{
							return step;
						}
					}
				}
				else if(!frontier.contains(adj))
				{
					/* If the current region is not a border and not yet
					 * in the frontier, 
					 */
					frontier.add(adj);
					reverseTree.put(adj.id, curr.id);
				}
			}
			
			currNum++;
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param opponentName The name of the opponent from BotState
	 * @return true iff an adjacent Region is owned by opponentName
	 */
	public boolean hasEnemy(String opponentName){
		for(Region r: neighbors){
			if(r.ownedByPlayer(opponentName))
				return true;
		}
		return false;
	}
	
}
