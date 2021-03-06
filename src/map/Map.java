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
import java.util.Random;

import bot.BotStarter;

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
		for(int i = 0; i < superRegions.size(); i++) //copy superRegions
		{
			SuperRegion sr = superRegions.get(i);
			SuperRegion newSuperRegion = new SuperRegion(sr.getId(), sr.getArmiesReward());
			newMap.add(newSuperRegion);
		}
		for(int i = 0; i < regions.size(); i++) //copy regions
		{
			Region r = regions.get(i);
			Region newRegion = new Region(r.getId(), newMap.getSuperRegion(r.getSuperRegion().getId()), r.getPlayerName(), r.getArmies());
			newMap.add(newRegion);
		}
		for(int i = 0; i <regions.size(); i++) //add neighbors to copied regions
		{
			Region r = regions.get(i);
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
		//Here are my thoughts:
		//going through each superregion and its subregions is O(n) where n is the number of total regions
		//its good to own a region, so add one
		//its great to own an entire super region, so add bonus (some better than others)
		//its better to own more of a super region
		//	-also ought to help plateau
		double util = 0;
		double ratio;
		int ownedRegions;
		for(SuperRegion sr: superRegions){
			
			ownedRegions = 0; //counter for current sr
			
			for(Region r: sr.getSubRegions()){
				if(r.ownedByPlayer(myName))ownedRegions++;
			}
			util += ownedRegions; //how many we own in sr 
			ratio = ((double)ownedRegions)/sr.getSubRegions().size();
			util += (ratio * ratio) * sr.getArmiesReward(); //less for lower ratios
			
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
			from = (int)(rand.nextDouble() * (deployed.length));
		} while(deployed[from] <= 0);
		
		//Get a region to move to, making sure that it is not the same as the from region
		do
		{
			to = (int)(rand.nextDouble() * (deployed.length));
		} while(to == from);
		
		deployed[from]--;
		deployed[to]++;
		
		this.getRegion(ids[from]).setArmies(this.getRegion(ids[from]).getArmies() - 1);
		this.getRegion(ids[to]).setArmies(this.getRegion(ids[to]).getArmies() + 1);
	}
	
	/** Simulates the outcome of a set of attacks
	 * @param fromId The region attacking from
	 * @param attacks The number of armies to attack each bordering
	 * @param toIds The ids of each border region
	 * @param playerName The current player's name
	 * TODO: make accurate
	 */
	public void simulateAttacks(int fromId, int[] attacks, int[] toIds, String playerName)
	{
		if(attacks.length != toIds.length) return;
		int attackers, defenders;
		
		for(int i = 0; i < attacks.length; i++)
		{
			if(toIds[i] != fromId)
			{
				attackers = attacks[i];
				defenders = this.getRegion(toIds[i]).getArmies();
				int attackersDestroyed = (int) (.7 * defenders); //note: these are expected values TODO: +1?
				int defendersDestroyed = (int)(.6 * attackers);  //true value is Gaussian, centered here
				
				if(defendersDestroyed >= defenders){//we predict a win, so its ours. also update armies
					this.getRegion(toIds[i]).setPlayerName(playerName);
					this.getRegion(toIds[i]).setArmies(attackers-attackersDestroyed);
					this.getRegion(fromId).setArmies(this.getRegion(fromId).getArmies() - attackers);
				}else{ //we predict loss for that attack. Just update armies
					attackersDestroyed = Math.min(attackers, attackersDestroyed); //enemy cannot defeat what did not attack
					//defendersDestroyed ought to be fine, as if would have caught it
					this.getRegion(fromId).setArmies(this.getRegion(fromId).getArmies() - attackersDestroyed);
					this.getRegion(toIds[i]).setArmies(this.getRegion(toIds[i]).getArmies()-defendersDestroyed);
				}
			}
		}
	}
	
	public void undoSimulation(int fromId, int[] attacks,int[] defenders, int[] toIds,String[]names, int totalOnAttacker){
		for(int i=0; i < toIds.length; i++){
			this.getRegion(toIds[i]).setArmies(defenders[i]);
			this.getRegion(toIds[i]).setPlayerName(names[i]);
		}
		this.getRegion(fromId).setArmies(totalOnAttacker);
	}
}