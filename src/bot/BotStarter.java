/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

/**
 * 
 * This class implements the Bot interface and overrides its Move methods.
 * You can implement these methods yourself very easily now,
 * since you can retrieve all information about the match from variable state.
 * When the bot decided on the move to make, it returns an ArrayList of Moves. 
 * The bot is started by creating a Parser to which you add
 * a new instance of your bot, and then the parser is started.
 */

import java.util.ArrayList;
import java.util.LinkedList;

import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotStarter implements Bot 
{
	@Override
	/**
	 * A method that returns which region the bot would like to start on, the pickable regions are stored in the BotState.
	 * The bots are asked in turn (ABBAABBAAB) where they would like to start and return a single region each time they are asked.
	 * This method returns the region with the least number of neighbors from the given pickable regions.
	 */
	public Region getStartingRegion(BotState state, Long timeOut)
	{
		int temp = 100, choice = 0, n;
		for(int i=0; i<state.getPickableStartingRegions().size();i++){
			n = state.getPickableStartingRegions().get(i).getNeighbors().size();
			if(n < temp)choice = i;
			temp = n;
		}
		int regionId = state.getPickableStartingRegions().get(choice).getId();
		Region startingRegion = state.getFullMap().getRegion(regionId);
		
		return startingRegion;
	}

	@Override
	/**
	 * This method is called for at first part of each round. 
	 * 
	 * The algorithm is as follows:
	 * 	Determine which regions are deployable
	 * 	Figure out how many regions we want to deploy at each region
	 * 	-deploy to places that need defending first
	 * 	-if the total amount of armies needed for defense is larger than the total we have
	 * 		-give all regions based on ratio to defense
	 *   otherwise give what is needed to defense, then give what is needed for each non-endangered territory for expansion	
	 * 	
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) 
	{
		
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		String myName = state.getMyPlayerName();
		String opponentName = state.getOpponentPlayerName();
		int totalWantedEnemy = 0;
		int armiesToDeploy = state.getStartingArmies();
		LinkedList<Region> visibleRegions = state.getVisibleMap().getRegions();
		ArrayList<Region> deployRegions = new ArrayList<Region>();
		
		//add to deployRegions deployable regions from visibleRegions. Namely, those that are yours and that are on the border
		for(int i = 0; i < visibleRegions.size(); i++){
			Region current = visibleRegions.get(i);
			if(current.ownedByPlayer(myName) && current.isBorder())
				deployRegions.add(current);
		}
		System.err.println(deployRegions.size() + " Deployable Regions. " + armiesToDeploy + " armies to deploy: \n");
		if(deployRegions.size() == 0){
			System.err.println("ERROR no deployable regions\n");
			return placeArmiesMoves;
		}
		//Figure out how many armies we "need" at each border location.
		//If there are enemies present, we want to have at least 1.5x enemy count
		//otherwise we want 2x the largest neutral army count (wastelands)
		for(int i = 0; i < deployRegions.size(); i++){
			Region current = deployRegions.get(i);
			if(current.hasEnemy(opponentName)){
				//We want to have 1.5x enemy count. If we have more than that already, we don't want any
				//find total enemies
				int totalEnemy = 0;
				for(Region r: current.getNeighbors()){
					if(r.ownedByPlayer(opponentName)) //make sure it is owned by enemy
						totalEnemy = totalEnemy + r.getArmies();
				}
				totalEnemy = (int) (totalEnemy * 1.5); //lets pretend they have more
				//If we do not have enough, we want the difference between what we need and what we have
				if(current.getArmies() < totalEnemy){
					current.setWantedArmies(totalEnemy - current.getArmies());
					totalWantedEnemy += current.getWantedArmies();
				}
				//set to 0 because otherwise the last iteration may persist!
				else{
					current.setWantedArmies(0);
				}
			}else{
				//There are no enemies, but it was a border. Therefore there are only neutral here.
				//We first find the maximum neutral army count.
				int maxNeut = 2; //they are default 2 unless there is a wasteland
				for(Region r: current.getNeighbors()){
					if(!r.ownedByPlayer(myName) && !r.ownedByPlayer(opponentName) && r.getArmies() > maxNeut)
						maxNeut = r.getArmies();
				}
				//Do we already have enough?
				maxNeut = 2 * maxNeut;
				if(maxNeut > current.getArmies()){
					current.setWantedArmies(maxNeut - current.getArmies());
				}
				//update to prevent persistance
				else{
					current.setWantedArmies(0);
				}
			}
		}
		
		
		//If we want more for enemies than we can give, then give all to those based on ratio
		if(totalWantedEnemy > armiesToDeploy){
			for(int i = 0; i < deployRegions.size(); i++){
				Region current = deployRegions.get(i);
				if(current.hasEnemy(opponentName))
					current.setWantedArmies((int) ((current.getWantedArmies()/(double)totalWantedEnemy) * armiesToDeploy));
				else
					current.setWantedArmies(0); //we are giving all to endangered areas
					//note the effect this has on truncated values: any remaining armies due to truncation are added
					//to a location(s) that was given armies
			} 
		}else{
			//all of our wanted values in endangered areas are correct already	
			int armiesLeft = armiesToDeploy - totalWantedEnemy;
				
			//for all of the neutral places we see in list, subtract the wanted value from the armiesLeft
			//if the result is greater than 0, continue, otherwise set wanted to 0
			for(int i = 0; i < deployRegions.size(); i++){
				Region current = deployRegions.get(i);
				if(!current.hasEnemy(opponentName)){
					armiesLeft = armiesLeft - current.getWantedArmies(); 
					if(armiesLeft < 0){
						current.setWantedArmies(0);
					}
				}
			}
		
		}
		//check and see how many we have allocated
		int allocated = 0;
		for(int i = 0; i < deployRegions.size(); i++){
			allocated += deployRegions.get(i).getWantedArmies();
		}
		
		//avoid infinite loop:
		//if we didnt NEED armies anywhere, disperse them amongst the edges
		if(allocated == 0){
			//for each of the border regions, just keep adding one to plan there until we are out
			int k = 0;
			while(allocated < armiesToDeploy){
				if(k < deployRegions.size()){
					deployRegions.get(k).setWantedArmies(deployRegions.get(k).getWantedArmies() + 1);
					allocated++;
					k++;
				}else{
					k = 0;
				}
			}
		}else{
		
			//until we have planned all remaining armies, go to every place we planned to deploy to and add an army
			int i = 0;
			while(allocated < armiesToDeploy){
				if(i < deployRegions.size()){
					if(deployRegions.get(i).getWantedArmies() > 0){
						deployRegions.get(i).setWantedArmies(deployRegions.get(i).getWantedArmies() + 1); //increment how many we plan to add
						allocated++;
					}
					i++; //move to next
				}else{
					i = 0; //go back to start of list
				}
			}
		}
		
		//Give our completed plans to the arraylist to be returned!
		for(int i=0; i < deployRegions.size(); i++){
			Region current = deployRegions.get(i);
			if(current.getWantedArmies() > 0){
				placeArmiesMoves.add(new PlaceArmiesMove(myName, current, current.getWantedArmies()));
				//DEBUG
				System.err.println("Tried placing " + current.getWantedArmies() +" on region " + current.getId());
			}
		}
		System.err.println("-------------END TURN--------------");
		//go get 'em boy!
		return placeArmiesMoves;
	}

	@Override
	/**
	 * This method is called for at the second part of each round. This example attacks if a region has
	 * more than 6 armies on it, and transfers if it has less than 6 and a neighboring owned region.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) 
	{
		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();
		String myName = state.getMyPlayerName();
		int armies = 0;
		int maxTransfers = 10;
		int transfers = 0;
		
		for(Region fromRegion : state.getVisibleMap().getRegions())
		{
			if(fromRegion.ownedByPlayer(myName)) //Do an attack or transfer
			{
//				// Old attack/transfer code
//				ArrayList<Region> possibleToRegions = new ArrayList<Region>();
//				possibleToRegions.addAll(fromRegion.getNeighbors());
//
//				while(!possibleToRegions.isEmpty())
//				{
//					double rand = Math.random();
//					int r = (int) (rand*possibleToRegions.size());
//					Region toRegion = possibleToRegions.get(r);
//					
//					if(!toRegion.getPlayerName().equals(myName) && fromRegion.getArmies() > 6) //do an attack
//					{
//						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, armies));
//						break;
//					}
//					else if(toRegion.getPlayerName().equals(myName) && fromRegion.getArmies() > 1
//								&& transfers < maxTransfers) //do a transfer
//					{
//						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, armies));
//						transfers++;
//						break;
//					}
//					else
//						possibleToRegions.remove(toRegion);
//				}
				
				//New transfer code
				//TODO: Test and debug this code in a match
				if(!fromRegion.isBorder() && fromRegion.getArmies() > 1 && transfers < maxTransfers)
				{
					Region nextStep = fromRegion.closestAdjacentToBorder();
					if(nextStep != null)
					{
						armies = fromRegion.getArmies() - 1;
						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, nextStep, armies));
						transfers++;
						break;
					}
				}
				//Random Attack code
				else if(fromRegion.isBorder() && fromRegion.getArmies() > 1){
					if(Math.random() > .5){
						//select a random non-self region
						Region selection = null;
						do{
							selection = fromRegion.getNeighbors().get((int) (Math.random()*fromRegion.getNeighbors().size()));
						}while(selection.ownedByPlayer(fromRegion.getPlayerName()));
						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, selection, fromRegion.getArmies() -1));
					}
				}
			}
		}
		
		return attackTransferMoves;
	}

	public static void main(String[] args)
	{
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
	}

}
