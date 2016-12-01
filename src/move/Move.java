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

package move;

public class Move {
	
	private String playerName; //name of the player that did this move
	private String illegalMove = ""; //gets the value of the error message if move is illegal, else remains empty
	
	/**
	 * @param playerName Sets the name of the Player that this Move belongs to
	 */
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}
	
	/**
	 * @param illegalMove Sets the error message of this move. Only set this if the Move is illegal.
	 */
	public void setIllegalMove(String illegalMove) {
		this.illegalMove = illegalMove;
	}
	
	/**
	 * @return The Player's name that this Move belongs to
	 */
	public String getPlayerName() {
		return playerName;
	}
	
	/**
	 * @return The error message of this Move
	 */
	public String getIllegalMove() {
		return illegalMove;
	}

}
