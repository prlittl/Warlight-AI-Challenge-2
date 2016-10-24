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
 * This is a simple bot that does random (but correct) moves.
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
	 * For each of the regions:
	 * 	if the region is not owned by the player remove it from the list
	 * 	For each of the regions adjacent to this region\
	 * 		Find sum of TotalEnemyAdjacentArmies and TotalNeutralAdjacent
	 *  if 0 sums, remove region from list and continue
	 *  otherwise	
	 * 	DeployHeuristic = -2(ThisRegionsArmies - TotalEnemyAdjacentArmies) + 3*TotalEnemyAdjacentArmies + TotalNeutralAdjacent
	 *  TotalSum +=DeployHeuristic
	 * Deploy armies based on the Ratio of each regions DeployHeuristic and the totalSum and the number of armies to deploy
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) 
	{
		
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		String myName = state.getMyPlayerName();
		int TotalSum = 0;
		int armiesToDeploy = state.getStartingArmies();
		LinkedList<Region> DeployRegions = state.getVisibleMap().getRegions();
		
		for(Region r: DeployRegions){
			if(!r.ownedByPlayer(myName))
				DeployRegions.remove(r);
			
			int totalEnemies =0, totalNeutral = 0;
			for(Region adjacent: r.getNeighbors()){
				if(adjacent.ownedByPlayer(myName)) continue;
				if(adjacent.ownedByPlayer(state.getOpponentPlayerName())){
					totalEnemies += adjacent.getArmies();
				}
				else totalNeutral += adjacent.getArmies();
			}
			if(totalEnemies == 0 && totalNeutral == 0)
				DeployRegions.remove(r);
			else{
				int val = -2*(r.getArmies() - totalEnemies) + 3*totalEnemies + totalNeutral;
				if(val < 0) val = 0;
				r.setDeployHeuristic(val);
				TotalSum += val;
			}
			
		}
		
		int planned = 0; //because of truncation, used later
		for(Region r: DeployRegions){
			int armies = (int) (r.getDeployHeuristic()/(double)TotalSum)*armiesToDeploy;
			placeArmiesMoves.add(new PlaceArmiesMove(myName, r, armies));
			planned += armies;
		}
		if(planned != armiesToDeploy){
			//add remainder to the max heuristic region
			int maxHeuristic = Integer.MIN_VALUE;
			int index = 0;
			for(int i = 0; i < DeployRegions.size(); i++){
				if(DeployRegions.get(i).getDeployHeuristic() > maxHeuristic){
					maxHeuristic = DeployRegions.get(i).getDeployHeuristic();
					index = i;
				}
			}
			//index of DeployRegions and placeArmiesMoves relate by the region
			placeArmiesMoves.get(index).setArmies(armiesToDeploy-planned);
			
		}
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
