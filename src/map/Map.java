/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package map;

import java.util.LinkedList;
import java.util.Random;

public class Map {
	
	public LinkedList<Region> regions;
	public LinkedList<SuperRegion> superRegions;
	
	public Map()
	{
		this.regions = new LinkedList<Region>();
		this.superRegions = new LinkedList<SuperRegion>();
	}
	
	public Map(LinkedList<Region> regions, LinkedList<SuperRegion> superRegions)
	{
		this.regions = regions;
		this.superRegions = superRegions;
	}

	/**
	 * add a Region to the map
	 * @param region : Region to be added
	 */
	public void add(Region region)
	{
		for(Region r : regions)
			if(r.getId() == region.getId())
			{
				System.err.println("Region cannot be added: id already exists.");
				return;
			}
		regions.add(region);
	}
	
	/**
	 * add a SuperRegion to the map
	 * @param superRegion : SuperRegion to be added
	 */
	public void add(SuperRegion superRegion)
	{
		for(SuperRegion s : superRegions)
			if(s.getId() == superRegion.getId())
			{
				System.err.println("SuperRegion cannot be added: id already exists.");
				return;
			}
		superRegions.add(superRegion);
	}
	
	/**
	 * @return : a new Map object exactly the same as this one
	 */
	public Map getMapCopy() {
		Map newMap = new Map();
		for(SuperRegion sr : superRegions) //copy superRegions
		{
			SuperRegion newSuperRegion = new SuperRegion(sr.getId(), sr.getArmiesReward());
			newMap.add(newSuperRegion);
		}
		for(Region r : regions) //copy regions
		{
			Region newRegion = new Region(r.getId(), newMap.getSuperRegion(r.getSuperRegion().getId()), r.getPlayerName(), r.getArmies());
			newMap.add(newRegion);
		}
		for(Region r : regions) //add neighbors to copied regions
		{
			Region newRegion = newMap.getRegion(r.getId());
			for(Region neighbor : r.getNeighbors())
				newRegion.addNeighbor(newMap.getRegion(neighbor.getId()));
		}
		return newMap;
	}
	
	/**
	 * @return : the list of all Regions in this map
	 */
	public LinkedList<Region> getRegions() {
		return regions;
	}
	
	/**
	 * @return : the list of all SuperRegions in this map
	 */
	public LinkedList<SuperRegion> getSuperRegions() {
		return superRegions;
	}
	
	/**
	 * @param id : a Region id number
	 * @return : the matching Region object
	 */
	public Region getRegion(int id)
	{
		for(Region region : regions)
			if(region.getId() == id)
				return region;
		return null;
	}
	
	/**
	 * @param id : a SuperRegion id number
	 * @return : the matching SuperRegion object
	 */
	public SuperRegion getSuperRegion(int id)
	{
		for(SuperRegion superRegion : superRegions)
			if(superRegion.getId() == id)
				return superRegion;
		return null;
	}
	
	public String getMapString()
	{
		String mapString = "";
		for(Region region : regions)
		{
			mapString = mapString.concat(region.getId() + ";" + region.getPlayerName() + ";" + region.getArmies() + " ");
		}
		return mapString;
	}	
	
	
	/**
	 * @param myName
	 * @param opponent
	 * @return the utility value of the map
	 */
	public double Utility(String myName, String opponent){
		double util = 0; //calculated utility value
		int owned = 0; //number of regions owned in a super region
		int enemyOwned = 0; //number of regions the enemy owns in a super region
		
		//for every region add one point of utility if we own it, and take one away if the enemy does
		for(int i=0;i<regions.size();i++){
			if(regions.get(i).getPlayerName().equals(myName))util++;
			else if(regions.get(i).getPlayerName().equals(opponent))util--;
		}
		
		//for every super region add to utility (total/owned regions) * the super region army bonus
		for(int i=0;i<superRegions.size();i++){
			//for every sub region of the super region see if it is owned by us or the opponent
			for(int j=0;j<superRegions.get(i).getSubRegions().size();j++){
				if(superRegions.get(i).getSubRegions().get(j).getPlayerName().equals(myName))owned++;
				else if(superRegions.get(i).getSubRegions().get(j).getPlayerName().equals(opponent))enemyOwned++;
			}
			
			util += ((double)owned/(superRegions.get(i).getSubRegions().size()) * superRegions.get(i).getArmiesReward());
			
			//if the enemy owns at least one region in a super region, take away one point for every one we own in that super region
			if(owned != superRegions.get(i).getSubRegions().size() && enemyOwned > 0)util -= owned;
			
			//if we own all regions in a super region, add an arbitrarily large bonus ******Jacob changed to be super region bonus amount
			if(owned == superRegions.get(i).getSubRegions().size())util += superRegions.get(i).getArmiesReward();
		}
		
		return util;
	}
	
	/**
	 * Randomly moves an army from one of the regions that has been deployed to
	 * to another border region
	 * @param ids The list of region ids corresponding to the regions deployed to
	 * @param deployed The number of armies deployed so far to each region
	 */
	public void getRandomSuccessor(int[] ids, int[] deployed)
	{
		//Make sure at least one deployment was made so far
		boolean deployMade = false;
		for(int i = 0; i < deployed.length; i++)
		{
			if(deployed[i] > 0) deployMade = true;
		}
		if(!deployMade || ids.length <= 1) return;
		
		//Create a random number stream
		Random rand = new Random(System.currentTimeMillis());
		
		//Get a region to move from, making sure that it has at least one deployed already
		int from, to;
		do
		{
			from = (int)(rand.nextDouble() * (deployed.length - 1));
		} while(deployed[from] == 0);
		
		//Get a region to move to, making sure that it is not the same as the from region
		do
		{
			to = (int)(rand.nextDouble() * (deployed.length - 1));
		} while(to == from);
		
		deployed[from]--;
		deployed[to]++;
		
		this.getRegion(ids[from]).setArmies(this.getRegion(ids[from]).getArmies() - 1);
		this.getRegion(ids[to]).setArmies(this.getRegion(ids[to]).getArmies() + 1);
	}
}
