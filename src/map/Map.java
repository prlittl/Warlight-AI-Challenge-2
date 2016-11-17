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
//		double util = 0; //calculated utility value
//		int owned = 0; //number of regions owned in a super region
//		int enemyOwned = 0; //number of regions the enemy owns in a super region
//		double enemyAttackers = 0;
//		
//		//for every region add one point of utility if we own it, and take one away if the enemy does
//		for(int i=0;i<regions.size();i++){
//			for(int j=0;j<regions.get(i).getNeighbors().size();j++){
//				if(regions.get(i).isBorder()){
//					if(regions.get(i).getNeighbors().get(j).getPlayerName().equals(opponent))enemyAttackers += regions.get(i).getNeighbors().get(j).getArmies();
//				}
//			}
//			//if a border region is in danger of being taken (probability greater than 0.5) do not include it in utility
//			if(regions.get(i).getPlayerName().equals(myName) && regions.get(i).isBorder() && BotStarter.probabilityToTake(enemyAttackers, regions.get(i).getArmies())> 0.6)
//				util -= BotStarter.probabilityToTake(enemyAttackers, regions.get(i).getArmies()); //TODO: Tweak decrease in utility here
//			else if(regions.get(i).getPlayerName().equals(myName))util++;
//			else if(regions.get(i).getPlayerName().equals(opponent))util--;
//			enemyAttackers = 0;
//		}
//		
//		//for every super region add to utility (total/owned regions) * the super region army bonus
//		for(int i=0;i<superRegions.size();i++){
//			//for every sub region of the super region see if it is owned by us or the opponent
//			for(int j=0;j<superRegions.get(i).getSubRegions().size();j++){
//				if(superRegions.get(i).getSubRegions().get(j).getPlayerName().equals(myName))owned++;
//				else if(superRegions.get(i).getSubRegions().get(j).getPlayerName().equals(opponent))enemyOwned++;
//			}
//			
//			util += ((double)owned/(superRegions.get(i).getSubRegions().size()) * superRegions.get(i).getArmiesReward());
//			
//			//if the enemy owns at least one region in a super region, take away one point for every one we own in that super region
//			if(owned != superRegions.get(i).getSubRegions().size() && enemyOwned > 0)util -= owned;
//			
//			//if we own all regions in a super region, add super region bonus amount
//			if(owned == superRegions.get(i).getSubRegions().size())util += superRegions.get(i).getArmiesReward();
//			
//			//find number of enemy attackers around a region
//			for(int j=0;j<superRegions.get(i).getSubRegions().size();j++){
//				for(int k=0;k<superRegions.get(i).getSubRegions().get(j).getNeighbors().size();k++){
//					if(superRegions.get(i).getSubRegions().get(j).isBorder()){
//						if(superRegions.get(i).getSubRegions().get(j).getNeighbors().get(k).getPlayerName().equals(opponent))enemyAttackers += superRegions.get(i).getSubRegions().get(j).getNeighbors().get(k).getArmies();
//					}
//				}
//			}
//			
//			//if the probability that the enemy takes a region in a super region we own is greater than
//			//0.5, then subtract the super region bonus from the utility
//			for(int j=0;j<superRegions.get(i).getSubRegions().size();j++){
//				if(owned == superRegions.get(i).getSubRegions().size()){
//					util -= superRegions.get(i).getArmiesReward() * BotStarter.probabilityToTake(enemyAttackers, superRegions.get(i).getSubRegions().get(j).getArmies());
//					break;
//				}
//			}
//			
//			owned = 0;
//			enemyOwned = 0;
//			enemyAttackers = 0;
//		}
//		
		//TODO: Real Utility Function
		double util = 0;
		int ownedRegions;
		for(int i=0;i<superRegions.size();i++){
			//for every sub region of the super region see if it is owned by us or the opponent
			ownedRegions = 0;
			for(int j=0;j<superRegions.get(i).getSubRegions().size();j++){
				if(superRegions.get(i).getSubRegions().get(j).getPlayerName().equals(myName)){
					ownedRegions++;
					util++;
				}
			}
			if (superRegions.get(i).getSubRegions().size() == ownedRegions){
				util += superRegions.get(i).getArmiesReward();
			}
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