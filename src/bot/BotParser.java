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

package bot;

import java.util.ArrayList;
import java.util.Scanner;

import map.Region;
import move.PlaceArmiesMove;
import move.AttackTransferMove;

public class BotParser {
	
	final Scanner scan;
	
	final Bot bot;
	
	BotState currentState;
	
	public BotParser(Bot bot)
	{
		this.scan = new Scanner(System.in);
		this.bot = bot;
		this.currentState = new BotState();
	}
	
	public void run()
	{
		while(scan.hasNextLine())
		{
			String line = scan.nextLine().trim();
			if(line.length() == 0) { continue; }
			String[] parts = line.split(" ");
			if(parts[0].equals("pick_starting_region")) //pick which regions you want to start with
			{
				currentState.setPickableStartingRegions(parts);
				Region startingRegion = bot.getStartingRegion(currentState, Long.valueOf(parts[1]));
				
				System.out.println(startingRegion.getId());
			}
			else if(parts.length == 3 && parts[0].equals("go")) 
			{
				//we need to do a move
				String output = "";
				if(parts[1].equals("place_armies")) 
				{
					//place armies
					ArrayList<PlaceArmiesMove> placeArmiesMoves = bot.getPlaceArmiesMoves(currentState, Long.valueOf(parts[2]));
					for(PlaceArmiesMove move : placeArmiesMoves)
						output = output.concat(move.getString() + ",");
				} 
				else if(parts[1].equals("attack/transfer")) 
				{
					//attack/transfer
					ArrayList<AttackTransferMove> attackTransferMoves = bot.getAttackTransferMoves(currentState, Long.valueOf(parts[2]));
					for(AttackTransferMove move : attackTransferMoves)
						output = output.concat(move.getString() + ",");
				}
				if(output.length() > 0)
					System.out.println(output);
				else
					System.out.println("No moves");
			} else if(parts[0].equals("settings")) {
				//update settings
				currentState.updateSettings(parts[1], parts);
			} else if(parts[0].equals("setup_map")) {
				//initial full map is given
				currentState.setupMap(parts);
			} else if(parts[0].equals("update_map")) {
				//all visible regions are given
				currentState.updateMap(parts);
			} else if(parts[0].equals("opponent_moves")) {
				//all visible opponent moves are given
				currentState.readOpponentMoves(parts);
			} else {
				System.err.printf("Unable to parse line \"%s\"\n", line);
			}
		}
	}

}
