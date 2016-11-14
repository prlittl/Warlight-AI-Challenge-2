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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import map.Map;
import map.Region;
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
	 * 	Find which regions we can deploy to
	 *  give a random initial configuration of deployed armies
	 *  Run Simulated Annealing for 500ms (the time alloted each round). Any time it takes over that to return 
	 *  	will be deducted from the 10000ms time bank
	 *  Prepare Return list and adjust current values of armies for the visible map.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) 
	{
		long startTime = System.nanoTime();
		double T = 500;
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		String myName = state.getMyPlayerName();
		String opponentName = state.getOpponentPlayerName();
		int armiesToDeploy = state.getStartingArmies();
		Map mapCopy = state.getVisibleMap().getMapCopy();
		
		LinkedList<Region> visibleRegions = mapCopy.getRegions();
		ArrayList<Region> deployableRegions = new ArrayList<Region>();
		//first, get our border regions and put them in deployable regions
		for(int i = 0; i < visibleRegions.size(); i++){
			if(visibleRegions.get(i).ownedByPlayer(myName) && visibleRegions.get(i).isBorder()){
				deployableRegions.add(visibleRegions.get(i));
			}
		}
		
		//set up int []'s for each of id's and planned deployment
		int[] ids = new int[deployableRegions.size()];
		int[] deployments = new int[deployableRegions.size()];
		
		//set up ids
		for(int i = 0; i < ids.length; i++){
			ids[i] = deployableRegions.get(i).getId();
		}
		//set up initial configuration
		
		double probability = 1.0/ids.length;
		int deployed = 0;
		int k = 0;
		
		while(deployed != armiesToDeploy){
			if(Math.random() < probability){
				deployments[k]++;
				deployed++;
			}
			k = (k+1) % ids.length;
		}
		//update mapCopy to match deployments
		for(int i = 0; i < ids.length; i++){
			mapCopy.getRegion(ids[i]).setArmies(mapCopy.getRegion(ids[i]).getArmies() + deployments[i]);
		}
		
		//get current Utility
		Map currentMap = mapCopy.getMapCopy();
		double currentUtil = expectedUtilityAfter(state, currentMap, myName);
		int [] curDeploy = new int[deployableRegions.size()];
		//set up loop
		double maxUtil = -Double.MAX_VALUE;
		
		int[] Max_deployments = Arrays.copyOf(deployments, deployments.length);
		
		while(true){
			if(T == 0) break; 
			curDeploy = Arrays.copyOf(deployments, deployments.length);
			currentMap = mapCopy.getMapCopy();
			currentMap.getRandomSuccessor(ids, curDeploy);
			//get the random successor's Utility
			double next = expectedUtilityAfter(state, mapCopy, myName);
			double deltaE = next - currentUtil;
			if(deltaE > 0){//current = next
				mapCopy = currentMap.getMapCopy();
				currentUtil = next;
				deployments = Arrays.copyOf(curDeploy, curDeploy.length);
				if(next > maxUtil){
					maxUtil = next;
					Max_deployments = Arrays.copyOf(deployments, deployments.length);
				}
			}else{
				double acceptProb = Math.exp(deltaE/T);
				if(Math.random() < acceptProb){
					mapCopy = currentMap.getMapCopy();
					currentUtil = next;
					deployments = Arrays.copyOf(curDeploy, curDeploy.length);
				}
			}
			
			T = computeT(startTime);
		}
		int armiesToDisperse = 0;
		//for those deployments which do not add any Utility, put those on another index > 0, if such exists
		for(int i = 0; i < Max_deployments.length; i++){
			mapCopy = state.getVisibleMap();
			double without = this.expectedUtilityAfter(state, mapCopy, myName);
			mapCopy.getRegion(ids[i]).setArmies(mapCopy.getRegion(ids[i]).getArmies() + Max_deployments[i]);
			double with = this.expectedUtilityAfter(state, mapCopy, myName);
			if(with <= without){
				armiesToDisperse += Max_deployments[i];
				Max_deployments[i] = 0;
			}
		}
		//find out if there are places to deploy to after removing useless deployments
		boolean canDisperse = false;
		for(int i = 0; i < Max_deployments.length; i++){
			if(Max_deployments[i] > 0){ canDisperse=true;break;}
		}
		if(canDisperse == false){
			Max_deployments[0] = armiesToDisperse;
		}else{
			k=0;
			while(armiesToDisperse > 0){ //spreads them about the better moves
				if(Max_deployments[k] > 0){
					Max_deployments[k]++;
					armiesToDisperse--;
				}
				k = (k+1) % Max_deployments.length;
			}
		}
		System.err.println("\nDEPLOYMENTS ROUND " + state.getRoundNumber() + " -----------------------------");
		for(int i = 0; i < ids.length; i++)
		{
			if(Max_deployments[i] > 0)
			{
				placeArmiesMoves.add(new PlaceArmiesMove(myName, state.getVisibleMap().getRegion(ids[i]), Max_deployments[i]));
				//add our plan to the total armies of the regions.
				state.getVisibleMap().getRegion(ids[i]).setArmies(state.getVisibleMap().getRegion(ids[i]).getArmies() + Max_deployments[i]);
			}
			System.err.println("Deployment of  " + Max_deployments[i] + " armies to region " + ids[i] + ".");
		}
		
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
		Map mapCopy = state.getVisibleMap().getMapCopy();
		String myName = state.getMyPlayerName();
		int armies = 0;
		
		//TODO: REMOVE DEBUG STATEMENT
		System.err.println("\nATTACKS ROUND " + state.getRoundNumber() + " ---------------------------------");
		
		for(Region fromRegion : state.getVisibleMap().getRegions())
		{
			if(fromRegion.ownedByPlayer(myName)) //Do an attack or transfer
			{
				//New transfer code
				//Tested
				if(!fromRegion.isBorder() && fromRegion.getArmies() > 1)
				{
					Region nextStep = fromRegion.closestAdjacentToBorder();
					if(nextStep != null)
					{
						armies = fromRegion.getArmies() - 1;
						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, nextStep, armies));
						break;
					}
				}
				//Attack
				else if(fromRegion.isBorder() && fromRegion.getArmies() > 1)
				{	
					//get list of regions I can attack
					ArrayList<Region> attackable = new ArrayList<Region>();
					for(int i = 0; i < fromRegion.getNeighbors().size(); i++){
						Region current = fromRegion.getNeighbors().get(i);
						if(!current.getPlayerName().equals(myName)){
							attackable.add(current);
						}
					}
					attackable.add(fromRegion);
					
					//set up int []'s for each of id's and planned attacks
					int[] ids = new int[attackable.size()];
					int[] attacks = new int[attackable.size()];
					
					//set up ids
					for(int i = 0; i < ids.length; i++){
						ids[i] = attackable.get(i).getId();
					}
					
					//set up initial configuration of attacks
					Map currMap;
					int[] currAttacks;
					double lastUtil = -Double.MAX_VALUE;
					double currUtil = -Double.MAX_VALUE;
					double probability = 1.0/ids.length;
					int attacked = 0;
					int k = 0;
					
					while(attacked != (fromRegion.getArmies() - 1)){
						if(Math.random() < probability){
							attacks[k]++;
							attacked++;
						}
						k = (k+1) % ids.length;
					}
					
					//Use a hill-climbing search to find the 'best' attack combination
					//TODO: Find a better way to limit iterations
					for(int i = 0; i < 2000; i++)
					{	
						//Create a random permutation of the attack
						currAttacks = Arrays.copyOf(attacks, attacks.length);
						randomPermutation(currAttacks);
						
						//Simulate the permutation of the attack and get its utility
						currMap = mapCopy.getMapCopy();
						currMap.simulateAttacks(fromRegion.getId(), currAttacks, ids, myName);
						currUtil = currMap.Utility(myName, state.getOpponentPlayerName());
						
						//If the permutation has better utility than before, accept it
						if(currUtil > lastUtil)
						{
							lastUtil = currUtil;
							attacks = currAttacks;
						}
					}
					
					//TODO: Remove debug statements
					if(attacks.length > 0)
					{
						System.err.println("Planned attacks from region " + fromRegion.getId() + ":");
						System.err.printf("IDs: %03d", ids[0]);
						for(int i = 1; i < ids.length; i++)
						{
							System.err.printf(", %03d", ids[i]);
						}
						System.err.println();
						
						System.err.printf("Atk: %03d", attacks[0]);
						for(int i = 1; i < attacks.length; i++)
						{
							System.err.printf(", %03d", attacks[i]);
						}
						System.err.println();
						
						System.err.printf("Def: %03d", state.getVisibleMap().getRegion(ids[0]).getArmies());
						for(int i = 1; i < ids.length; i++)
						{
							System.err.printf(", %03d", state.getVisibleMap().getRegion(ids[i]).getArmies());
						}
						System.err.println();
					}
					
					//Take the attack actions decided
					for(int i = 0; i < attacks.length; i++)
					{
						if(fromRegion.getId() != ids[i] && attacks[i] > 0)
						{
							attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, state.getVisibleMap().getRegion(ids[i]), attacks[i]));						
						}
					}
					mapCopy.simulateAttacks(fromRegion.getId(), attacks, ids, myName);
					
//					//Old/Tested attack code
//					
//					//I need two doubles for each Attackable Region: probability and Utility
//					//index of each corresponds to index of an attackable
//					double[] probabilities = new double[attackable.size()];
//					double[] utilities = new double[attackable.size()];
//					
//					//find each of the probabilities and utilities for taking an attackable region
//					for(int i = 0; i < attackable.size(); i++) {
//						Region current = attackable.get(i);
//						probabilities[i] = probabilityToTake(fromRegion.getArmies()-1,current.getArmies());
//						//compute the Utility of the map if I do take it
//						//does not take into account armies if that matters in future
//						Map visible = state.getVisibleMap();
//						//make the region in question mine **this should work...I think
//						for(int k = 0; k < visible.regions.size(); k++){
//							if(visible.regions.get(k).getId() == current.getId()){
//								String regionOwner = visible.regions.get(k).getPlayerName();
//								visible.regions.get(k).setPlayerName(myName);
//								utilities[i] = visible.Utility(myName, state.getOpponentPlayerName());
//								visible.regions.get(k).setPlayerName(regionOwner); //restore order. Don't want to consider it as ours next time!
//								break; //we got it.
//							}
//						}
//						
//						//TODO: REMOVE DEBUG STATEMENT
//						System.err.println((fromRegion.getArmies() - 1) + " attack "+attackable.get(i).getArmies() + ": Possible attack from region " + fromRegion.getId() + " to region " + attackable.get(i).getId() + " with probability " + probabilities[i] + " and utility " + utilities[i] + ".");
//					}
//					boolean[] willingToTry = new boolean[attackable.size()];
//					//give answer for each index
//					for(int i = 0; i<probabilities.length; i++){
//						if(probabilities[i] > .675) willingToTry[i] = true; //TODO prob
//						else willingToTry[i] = false; //I think array constructor may do this but I'll be safe
//					}
//					//find the max utility of the ones we are willing to try.
//					int indexMax = -1;
//					double maxUtil = -Double.MAX_VALUE;
//					for(int i = 0; i < utilities.length; i++){
//						if(willingToTry[i] && utilities[i] > maxUtil){
//							indexMax = i;
//							maxUtil = utilities[i];
//						}
//					}
//					//System.err.println("\n ATTACK: \n");
//					//for(Region r: attackable){
//					//	System.err.println(r.getId() + " is attackable" );
//					//}
//					//If we were willing to do any, do that best one with all we got.
//					if(indexMax != -1){
//						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, attackable.get(indexMax), fromRegion.getArmies()-1));
//					}
				}
			}
		}
		
		//System.err.println("\n Here is what we want to attack: ");
		//for(int i = 0; i < attackTransferMoves.size(); i++){
		//	System.err.println(attackTransferMoves.get(i).getFromRegion().getId() + " to " + attackTransferMoves.get(i).getToRegion().getId() + " with " + attackTransferMoves.get(i).getArmies());
		//}
		
		//System.err.println("-------------END TURN--------------");
		return attackTransferMoves;
	}
	
	//note that "expected" may not be the best term, as it doesn't actually take into account probability 
	//except to include a region after attack
	/**
	 * 
	 * @param state botstate
	 * @param visible bots
	 * @param myName playerName
	 * @return
	 */
	private double expectedUtilityAfter(BotState state, Map vis, String myName){
		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();
		Map visible = vis.getMapCopy();
		double CurrentUtility = visible.Utility(myName, state.getOpponentPlayerName());
		ArrayList<Double> probList = new ArrayList<Double>();
		for(Region fromRegion : visible.getRegions())
		{
			if(fromRegion.ownedByPlayer(myName)) //Do an attack or transfer
			{
				//Attack
				if(fromRegion.isBorder() && fromRegion.getArmies() > 1){ 
					
					//get list of regions I can attack
					ArrayList<Region> attackable = new ArrayList<Region>();
					for(int i = 0; i < fromRegion.getNeighbors().size(); i++){
						Region current = fromRegion.getNeighbors().get(i);
						if(!current.getPlayerName().equals(myName)){
							attackable.add(current);
						}
					}
					
					//I need two doubles for each Attackable Region: probability and Utility
					//index of each corresponds to index of an attackable
					double[] probabilities = new double[attackable.size()];
					double[] utilities = new double[attackable.size()];
					
					//find each of the probabilities and utilities for taking an attackable region
					for(int i =0;i< attackable.size(); i++){
						Region current = attackable.get(i);
						probabilities[i] = probabilityToTake(fromRegion.getArmies()-1,current.getArmies());
						//compute the Utility of the map if I do take it
						//does not take into account armies if that matters in future
						Map visible1 = state.getVisibleMap();
						//make the region in question mine **this should work...I think
						for(int k = 0; k < visible1.regions.size(); k++){
							if(visible1.regions.get(k).getId() == current.getId()){
								String regionOwner = visible1.regions.get(k).getPlayerName();
								visible1.regions.get(k).setPlayerName(myName);
								utilities[i] = visible1.Utility(myName, state.getOpponentPlayerName());
								visible1.regions.get(k).setPlayerName(regionOwner); //restore order. Don't want to consider it as ours next time!
								break; //we got it.
							}
						}
						
					}
					boolean[] willingToTry = new boolean[attackable.size()];
					//give answer for each index
					for(int i = 0; i<probabilities.length; i++){
						if(probabilities[i] > .675) willingToTry[i] = true; //TODO prob
						else willingToTry[i] = false; //I think array constructor may do this but I'll be safe
					}
					//find the max utility of the ones we are willing to try.
					int indexMax = -1;
					double maxUtil = -Double.MAX_VALUE;
					for(int i = 0; i < utilities.length; i++){
						if(willingToTry[i] && utilities[i] > maxUtil){
							indexMax = i;
							maxUtil = utilities[i];
						}
					}
					//If we were willing to do any, do that best one with all we got.
					if(indexMax != -1){
						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, attackable.get(indexMax), fromRegion.getArmies()-1));
						probList.add(probabilities[indexMax]);
					}
					
				}
			}
		}
		String opponentName = state.getOpponentPlayerName();
		//go through and make attacking regions our own, calculate the difference in utility
		//sum the differences in utility * prob of each
		//in this way, the bot will favor certainty
		double sum = 0;
		
		for(int i = 0;i<attackTransferMoves.size(); i++){
			AttackTransferMove move = attackTransferMoves.get(i);
			String playerName = move.getToRegion().getPlayerName();
			visible.getRegion(move.getToRegion().getId()).setPlayerName(myName);
			sum += (visible.Utility(myName, opponentName)-CurrentUtility) * probList.get(i); //(new-old)*probNew; expected gain
			//return to previous state TODO: armies changes???
			visible.getRegion(move.getToRegion().getId()).setPlayerName(playerName);
		}
		
		
		return sum;
	}
	
	public static double probabilityToTake(double attackers, double defenders){
		double probability = 0;
		
		//if they have less than .84*(.6*attackers), then we take them for sure => .504 * attackers = #certain defender death
		if(defenders< attackers*.504){
			probability = 1;
		}
		//if they have more than .84*(.6*attackers)+.16*attackers, then there is no way we can take them
		else if(defenders > (attackers*.504 + .16*attackers)+1){
			probability = 0; // already is but ok
		}
		//now we gotta find the legit probability...because there is a chance but its not certain...
		else{
			int need = (int) ((defenders - .504*attackers)/.16 ) ;
			if(need <= attackers)
			probability = 1 - binomialcdf(need, (int) attackers, .6);
			
		}
		return probability;
	}
	
	public static double binomialcdf(int numCorrect, int numTrials, double probValue){
		if(numCorrect == numTrials) return binomialDist(3,3,.6);
		double total = 0;
		for(int i = 0; i <= numCorrect; i++){
			total+=binomialDist(i, numTrials, probValue);
		}
		return total;
	}
	
	public static double binomialDist(int numCorrect, int numTrials, double probValue)
	{
		BigInteger ntF = factorial(numTrials);
		BigInteger denom = factorial(numCorrect).multiply(factorial(numTrials - numCorrect));

		BigDecimal ntFBD = new BigDecimal(ntF);
		BigDecimal denomBD = new BigDecimal(denom);
		BigDecimal quotient = ntFBD.divide(denomBD, 40, RoundingMode.HALF_UP);

		BigDecimal restBD = BigDecimal.valueOf(Math.pow(probValue, numCorrect) * Math.pow((1d - probValue), numTrials - numCorrect));
		return(quotient.multiply(restBD).doubleValue());
	}
	
	/**
	 * Compute factorial of n
	 */
	public static BigInteger factorial(int n)
	{
		BigInteger res = BigInteger.ONE;

		for (int i = n; i>1; i--)
		{
			res = res.multiply(BigInteger.valueOf(i));
		}
		return(res);
	}
	
	/**
	 * 
	 * @param startTime time turn started
	 * @return the value of T for current time
	 */
	public static double computeT(long startTime){
		long current = System.nanoTime();
		long diff = (long) ((current - startTime ) / 1000000.0);
        if(diff > 500){
        	return 0;
        }
        if(diff != 0)
        	return -1 + (500.0/diff); //TODO change?
        else return 1; //it happened SUPER quick
	}
	
	/** Subtracts one from a random array element and adds one to another
	 * @param arr The array to permutate
	 */
	public static void randomPermutation(int[] arr)
	{
		//Make sure at least one element is nonzero
		boolean nonzero = false;
		for(int i = 0; i < arr.length; i++)
		{
			if(arr[i] > 0) nonzero = true;
		}
		if(!nonzero || arr.length <= 1) return;
		
		//Create a random number stream
		Random rand = new Random(System.currentTimeMillis());
		
		//Get an index to move from, making sure that it is at least one
		int from, to;
		do
		{
			from = (int)(rand.nextDouble() * (arr.length));
		} while(arr[from] <= 0);
		
		//Get an index to move to, making sure that it is not the same region as before
		do
		{
			to = (int)(rand.nextDouble() * (arr.length));
		} while(to == from);
		
		arr[from]--;
		arr[to]++;
	}
	
	public static void main(String[] args)
	{
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
	}

}
