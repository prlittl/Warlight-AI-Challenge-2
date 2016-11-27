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
import java.util.Queue;
import java.util.Random;

import map.Map;
import map.Region;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotStarter implements Bot 
{
	boolean firstRegion = true; // boolean to see if the bot is picking its first starting region
	boolean visited[]; //list of visited nodes for bfs
	Region cRegion; //current root region for bfs
	
	@Override
	/**
	 * A method that returns which region the bot would like to start on, the pickable regions are stored in the BotState.
	 * The bots are asked in turn (ABBAABBAAB) where they would like to start and return a single region each time they are asked.
	 * This method returns the region with the least number of neighbors from the given pickable regions.
	 */
	public Region getStartingRegion(BotState state, Long timeOut)
	{
		int temp = 100, choice = 0, n;
		Region currRegion; //current region for bfs
		
		if(firstRegion){
			for(int i=0; i<state.getPickableStartingRegions().size();i++){
				n = state.getPickableStartingRegions().get(i).getNeighbors().size();
				if(n < temp)choice = i;
				temp = n;
			}	
			int regionId = state.getPickableStartingRegions().get(choice).getId();
			Region startingRegion = state.getFullMap().getRegion(regionId);
			firstRegion = false;
			cRegion = startingRegion;
			visited = new boolean[state.getFullMap().getRegions().size()]; //assuming region ids correspond to size of map
			return startingRegion;
		}
		else{
			// BFS
			Queue<Region> q = new LinkedList<Region>();
			q.add(cRegion);
			while(!q.isEmpty()){
				currRegion = q.remove();
				
				if(!visited[currRegion.getId()]){
					visited[currRegion.getId()] = true;
					for(int i=0;i<currRegion.getNeighbors().size();i++){
						for(int j=0;j<state.getPickableStartingRegions().size();j++){
							if(currRegion.getNeighbors().get(i).equals(state.getPickableStartingRegions().get(j))){
								cRegion = state.getPickableStartingRegions().get(j);
								return state.getPickableStartingRegions().get(j);
							}
						}
						if(!visited[currRegion.getNeighbors().get(i).getId()])q.add(currRegion.getNeighbors().get(i));
					}
				}	
			}
			System.err.println("---------BFS Not Working--------");
			return null;
		}
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
		
		
		//get current Utility
		applyDeployments(mapCopy, deployments, ids);
		double currentUtil = expectedUtilityAfter(state, mapCopy, myName);
		deapplyDeployments(mapCopy, deployments, ids);
		
		int [] oldDeploy = new int[deployments.length];
		//set up loop
		double maxUtil = -Double.MAX_VALUE;
		
		int[] Max_deployments = Arrays.copyOf(deployments, deployments.length);
		int count = 0;
		double next, deltaE;
		while(true){
			if(T == 0) break; 
			oldDeploy = Arrays.copyOf(deployments, deployments.length);
			
			randomPermutation(deployments);
			applyDeployments(mapCopy, deployments, ids);
			next = expectedUtilityAfter(state, mapCopy, myName);
			deapplyDeployments(mapCopy, deployments, ids);
			
			//get the random successor's Utility
			
			deltaE = next - currentUtil;
			if(deltaE > 0){//old = new
				currentUtil = next;
				
				if(next > maxUtil){
					maxUtil = next;
					Max_deployments = Arrays.copyOf(deployments, deployments.length);
				}
			}else{
				double acceptProb = Math.exp(deltaE/T);
				if(Math.random() < acceptProb){
					currentUtil = next;
				}else{
					deployments = oldDeploy;
				}
			}
			count++;
			T = computeT(startTime,200);
		}
		//TODO: remove debug
		System.err.println(count + " Simulated Annealing Loops done******");
		int armiesToDisperse = 0;
		//for those deployments which do not add any Utility, put those on another index > 0, if such exists
		for(int i = 0; i < Max_deployments.length; i++){
			
			double without = this.expectedUtilityAfter(state, mapCopy, myName);
			mapCopy.getRegion(ids[i]).setArmies(mapCopy.getRegion(ids[i]).getArmies() + Max_deployments[i]);
			double with = this.expectedUtilityAfter(state, mapCopy, myName);
			mapCopy.getRegion(ids[i]).setArmies(mapCopy.getRegion(ids[i]).getArmies() - Max_deployments[i]);
			
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
	//	System.err.println("\nDEPLOYMENTS ROUND " + state.getRoundNumber() + " -----------------------------");
		for(int i = 0; i < ids.length; i++)
		{
			if(Max_deployments[i] > 0)
			{
				placeArmiesMoves.add(new PlaceArmiesMove(myName, state.getVisibleMap().getRegion(ids[i]), Max_deployments[i]));
				//add our plan to the total armies of the regions.
				state.getVisibleMap().getRegion(ids[i]).setArmies(state.getVisibleMap().getRegion(ids[i]).getArmies() + Max_deployments[i]);
			}
			//System.err.println("Deployment of  " + Max_deployments[i] + " armies to region " + ids[i] + ".");
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
		String opponentName = state.getOpponentPlayerName();
		
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
					int fromId = fromRegion.getId();
					int[] ids = new int[attackable.size()];
					String[] names = new String[ids.length];
					int[] attacks = new int[ids.length];
					int[] defenders = new int[ids.length];
					int totalOnAttacker = fromRegion.getArmies();
					//set up ids, playernames, and defenders
					for(int i = 0; i < ids.length; i++){
						ids[i] = attackable.get(i).getId();
						names[i] = attackable.get(i).getPlayerName();
						defenders[i] = attackable.get(i).getArmies();
					}
					
					//set up initial configuration of attacks
					
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
					long startTime = System.nanoTime();
					double T = 500;
					double deltaE = 0;
					double maxUtil = -Double.MAX_VALUE;
					int[] maxAttacks = Arrays.copyOf(attacks, attacks.length);
					//Use a simulated Annealing search to find the 'best' attack combination
					
					while(true)
					{	
						if(T == 0) break;
						//Create a random permutation of the attack
						currAttacks = Arrays.copyOf(attacks, attacks.length);
						for(int i = 0; i <= fromRegion.getArmies()/5; i++)//TODO: modify?
							randomPermutation(currAttacks);
						
						//Simulate the permutation of the attack and get its utility
						
						mapCopy.simulateAttacks(fromId, currAttacks, ids, myName);
						currUtil = mapCopy.Utility(myName, opponentName);
						mapCopy.undoSimulation(fromId, currAttacks, defenders, ids, names, totalOnAttacker);
						
						
						deltaE = currUtil - lastUtil;
						//If the permutation has better utility than before, accept it
						if(deltaE > 0)
						{
							lastUtil = currUtil;
							attacks = currAttacks;
							if(currUtil > maxUtil){
								maxUtil = currUtil;
								maxAttacks = Arrays.copyOf(attacks, attacks.length);
							}
						}else {
							double acceptProb = Math.exp(deltaE/T);
							if(Math.random() < acceptProb){
								lastUtil = currUtil;
								attacks = currAttacks;
							
							}
						}
						T = computeT(startTime,300);
					}
					attacks = maxAttacks;
					
					//Sanity checks on attacks
					for(int i = 0; i < attacks.length - 1; i++)
					{
						//If there is a low chance to take any region, don't do the attack
						if(probabilityToTake(attacks[i],state.getVisibleMap().getRegion(ids[i]).getArmies()) < 0.35)
						{
							attacks[attacks.length - 1] += attacks[i];
							attacks[i] = 0;
						}
					}
					
					//Try to make other attacks which are close to being successful be successful
					boolean attackMade = false;
					for(int i = 0; i < attacks.length - 1; i++)
					{
						if(attacks[i] != 0)
						{
							//If we can take a region with greater than 0.6125 chance, make the attack
							if(probabilityToTake(attacks[i],state.getVisibleMap().getRegion(ids[i]).getArmies()) > 0.6125)
							{
								attackMade = true;
							}
							else
							{
								//Otherwise, if we have enough remaining armies to make the attack, take it
								if(probabilityToTake(attacks[i] + attacks[attacks.length - 1], state.getVisibleMap().getRegion(ids[i]).getArmies()) > 0.6125)
								{
									//Give just enough armies to pass 0.6125 threshold
									while(probabilityToTake(attacks[i], state.getVisibleMap().getRegion(ids[i]).getArmies()) <= 0.6125)
									{
										attacks[i]++;
										attacks[attacks.length - 1]--;
									}
									attackMade = true;
								}
								else
								{
									//If we don't have a good chance of taking even with extra armies, don't attack
									attacks[attacks.length - 1] += attacks[i];
									attacks[i] = 0;
								}
							}
						}
					}
					
					//Distribute remaining armies if an attack was made
					if(attackMade && attacks[attacks.length - 1] > 0)
					{
						int index = 0;
						while(attacks[attacks.length - 1] > 0)
						{
							if(attacks[index] > 0)
							{
								attacks[index]++;
								attacks[attacks.length - 1]--;
							}
							index = (index + 1) % (attacks.length - 1);
						}
					}
					
					//Take the attack actions decided
					for(int i = 0; i < attacks.length; i++)
					{
						if(fromRegion.getId() != ids[i] && attacks[i] > 0)
						{
							attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, state.getVisibleMap().getRegion(ids[i]), attacks[i]));						
						}
					}
				}
			}
		}
		
		//System.err.println("\n Here is what we want to attack: ");
		//for(int i = 0; i < attackTransferMoves.size(); i++){
		//	System.err.println(attackTransferMoves.get(i).getFromRegion().getId() + " to " + attackTransferMoves.get(i).getToRegion().getId() + " with " + attackTransferMoves.get(i).getArmies());
		//}
		
		//System.err.println("-------------END TURN--------------");
		
		//super bm
		int visRegionCount = 0;
		int totalRegionCount = state.getFullMap().getRegions().size();
		int theirRegions = 0;
		int bmCount = 0;
		
		for(int i=0;i<state.getVisibleMap().getRegions().size();i++){
			if(state.getVisibleMap().getRegions().get(i).getPlayerName().equals(opponentName))theirRegions++;
			visRegionCount++;
		}
		
		//TODO: make bot sit on deployments until final move, in which it attacks with all
		if(totalRegionCount == visRegionCount && theirRegions == 1 && bmCount <=10 && state.getRoundNumber() < 55){
			System.err.println("BM BM BM BM BM BM BM BM BM BM BM BM BM\n");
			return new ArrayList<AttackTransferMove>();
		}
		
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
		Map mapCopy = vis.getMapCopy();
		String opponentName = state.getOpponentPlayerName();
		for(Region fromRegion : mapCopy.getRegions())
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
					attackable.add(fromRegion);
					
					//set up int []'s for each of id's and planned attacks
					int fromId = fromRegion.getId();
					int[] ids = new int[attackable.size()];
					String[] names = new String[ids.length];
					int[] attacks = new int[ids.length];
					int[] defenders = new int[ids.length];
					int totalOnAttacker = fromRegion.getArmies();
					//set up ids, playernames, and defenders
					for(int i = 0; i < ids.length; i++){
						ids[i] = attackable.get(i).getId();
						names[i] = attackable.get(i).getPlayerName();
						defenders[i] = attackable.get(i).getArmies();
					}
					
					//set up initial configuration of attacks
					
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
					int[] maxAttacks = Arrays.copyOf(attacks, attacks.length);
					double maxUtil = -Double.MAX_VALUE;
					
					//Use a simulated annealing search to find the 'best' attack combination
					for(int i = 0; i < 250; i++)
					{	
						//Create a random permutation of the attack
						currAttacks = Arrays.copyOf(attacks, attacks.length);
						
						for(int l = 0; l <= fromRegion.getArmies()/2; l++) //TODO: modify?
							randomPermutation(currAttacks);
						
						//Simulate the permutation of the attack and get its utility
						mapCopy.simulateAttacks(fromId, currAttacks, ids, myName);
						currUtil = mapCopy.Utility(myName, opponentName);
						mapCopy.undoSimulation(fromId, currAttacks, defenders, ids, names, totalOnAttacker);
						
						
						//If the permutation has better utility than before, accept it
						if(currUtil > lastUtil)
						{
							lastUtil = currUtil;
							attacks = currAttacks;
							if(currUtil > maxUtil){
								maxUtil = currUtil;
								maxAttacks = Arrays.copyOf(attacks, attacks.length);
							}
						}else {
							if(Math.random() > i/240.0){
								lastUtil = currUtil;
								attacks=currAttacks;
							}
						}
					}
					attacks = maxAttacks;
					
					
					for(int i = 0; i < attacks.length - 1; i++)
					{
						//If there is a low chance to take any region, don't do the attack
						if(probabilityToTake(attacks[i],state.getVisibleMap().getRegion(ids[i]).getArmies()) < 0.35)
						{
							attacks[attacks.length - 1] += attacks[i];
							attacks[i] = 0;
						}
					}
					
					//Try to make other attacks which are close to being successful be successful
					boolean attackMade = false;
					for(int i = 0; i < attacks.length - 1; i++)
					{
						if(attacks[i] != 0)
						{
							//If we can take a region with greater than 0.6125 chance, make the attack
							if(probabilityToTake(attacks[i],state.getVisibleMap().getRegion(ids[i]).getArmies()) > 0.6125)
							{
								attackMade = true;
							}
							else
							{
								//Otherwise, if we have enough remaining armies to make the attack, take it
								if(probabilityToTake(attacks[i] + attacks[attacks.length - 1], state.getVisibleMap().getRegion(ids[i]).getArmies()) > 0.6125)
								{
									//Give just enough armies to pass 0.6125 threshold
									while(probabilityToTake(attacks[i], state.getVisibleMap().getRegion(ids[i]).getArmies()) <= 0.6125)
									{
										attacks[i]++;
										attacks[attacks.length - 1]--;
									}
									attackMade = true;
								}
								else
								{
									//If we don't have a good chance of taking even with extra armies, don't attack
									attacks[attacks.length - 1] += attacks[i];
									attacks[i] = 0;
								}
							}
						}
					}
					
					//Distribute remaining armies if an attack was made
					if(attackMade && attacks[attacks.length - 1] > 0)
					{
						int index = 0;
						while(attacks[attacks.length - 1] > 0)
						{
							if(attacks[index] > 0)
							{
								attacks[index]++;
								attacks[attacks.length - 1]--;
							}
							index = (index + 1) % (attacks.length - 1);
						}
					}
					
					
					
					
					//Take the attack actions decided
					for(int i = 0; i < attacks.length; i++)
					{
						if(fromRegion.getId() != ids[i] && attacks[i] > 0)
						{
							attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, state.getVisibleMap().getRegion(ids[i]), attacks[i]));						
						}
					}
				}
			}
		}
		
		
		//go through and make attacking regions our own, calculate the difference in utility
		//sum the differences in utility * prob of each
		//in this way, the bot will favor certainty
		double sum = 0;
		
		for(int i = 0;i<attackTransferMoves.size(); i++){
			AttackTransferMove move = attackTransferMoves.get(i);
			String playerName = move.getToRegion().getPlayerName();
			mapCopy.getRegion(move.getToRegion().getId()).setPlayerName(myName);
			sum += (mapCopy.Utility(myName, opponentName)) * probabilityToTake(move.getArmies(), move.getToRegion().getArmies()); //(new-old)*probNew; expected gain
			//return to previous state 
			mapCopy.getRegion(move.getToRegion().getId()).setPlayerName(playerName);
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
	public static double computeT(long startTime, int time){
		long current = System.nanoTime();
		long diff = (long) ((current - startTime ) / 1000000.0);
        if(diff > time){
        	return 0;
        }
        if(diff != 0)
        	return -1 + (time/diff); 
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
		} while(arr[from] < 1);
		
		//Get an index to move to, making sure that it is not the same region as before
		do
		{
			to = (int)(rand.nextDouble() * (arr.length));
		} while(to == from);
		
		arr[from]--;
		arr[to]++;
	}
	
	public static void mutateAttacks(int [] attacks){
		
	}
	
	public static void applyDeployments(Map map, int[] deployments, int[] ids){
		for(int i = 0; i < ids.length; i++){
			map.getRegion(ids[i]).setArmies(map.getRegion(ids[i]).getArmies() + deployments[i]);
		}
	}
	public static void deapplyDeployments(Map map, int[] deployments, int[] ids){
		for(int i = 0; i < ids.length; i++){
			map.getRegion(ids[i]).setArmies(map.getRegion(ids[i]).getArmies() - deployments[i]);
		}
	}
	public static void main(String[] args)
	{
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
	}

}
