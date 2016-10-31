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
	
	
}
