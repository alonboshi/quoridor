package quoridor;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GameState {
	static final int BOARD_SIZE = 9;
	static final char PLAYER_1_ICON = 'A';
	static final char PLAYER_2_ICON = 'B';
	/**
	 * A hybrid graph representation as suggested by van Rossum and Cormen et al:
	 * A hash table is used to associate each vertex with a doubly linked list of adjacent vertices
	 */
	protected HashMap <Square,LinkedList<Square>> adjacencyList = new HashMap <Square,LinkedList<Square>> ();
	// This is used for printing. Could be done away with with a little thought.
	protected HashMap <Square,Orientation> wallLookup = new HashMap<Square,Orientation>();
	Square player1Square = new Square("e9");
	Square player2Square = new Square("e1");
	LinkedList <Wall> walls = new LinkedList<Wall>();
	int numWalls1 = 0;
	int numWalls2 = 0;
	int turn = 0;

	public GameState() {
		// Initialize adjacency list
		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				LinkedList<Square> adjacent = new LinkedList<Square>();
				for (int d = -1; d < 2; d++) {
					if (d != 0) { // Vertices are not self-connecting
						if (i+d >= 0 && i+d < BOARD_SIZE)
							adjacent.add(new Square(i+d,j));
						if (j+d >= 0 && j+d < BOARD_SIZE)
							adjacent.add(new Square(i,j+d));
					}
				}
				adjacencyList.put(new Square(i,j), adjacent);
			}
		}
	}

	public GameState(GameState gs) {
		for (Square sq:gs.adjacencyList.keySet()) {
			LinkedList<Square> list = new LinkedList<Square>();
			list.addAll(gs.adjacencyList.get(sq));
			adjacencyList.put(sq, list);
		}
		wallLookup = new HashMap<Square,Orientation>(gs.wallLookup);
		player1Square = new Square(gs.player1Square);
		player2Square = new Square(gs.player2Square);
		walls.addAll(gs.walls);
		turn = gs.turn;
	}
	
	public GameState(List <String> moves) {
		new GameState();
		for (String e:moves) {
			move(e);
		}
	}
	
	// TODO: Need to check preconditions
	public void addEdge (Square a, Square b) {
		// Edges are undirected
		adjacencyList.get(a).add(b);
		adjacencyList.get(b).add(a);
	}

	/**
	 * The player who's turn it is.
	 * @return 0 if player 1, 1 if player 2.
	 */
	public Integer currentPlayer () {
		return turn%2;
	}

	public Square currentPlayerPosition () {
		return currentPlayer () == 0 ? player1Square : player2Square;
	}
	
	protected void displayAdjacencyList () {
		for (Square e:adjacencyList.keySet()) {
			System.out.println(e+": "+adjacencyList.get(e));
		}
	}

	public boolean hasPathToGoal () {
		return !(shortestPathToRow(player1Square, 0).isEmpty() || shortestPathToRow(player2Square, 8).isEmpty());
	}

	private boolean hasPlayer (int i, int j) {
		// TODO Should restrict return values to be injective. (The rounding down of division could is potentially problematic).  
		Square transformedPosition = new Square((i-1)>>1,(j-1)>>1);
		return player1Square.equals(transformedPosition) || player2Square.equals(transformedPosition);
	}

	private boolean hasWall (int i, int j) {
		return wallLookup.containsKey(new Square(i, j));
	}

	public int heuristic() {
		int score = 0;
		if (currentPlayer() == 0) {
			score = shortestPathToRow(player1Square, 0).size()-shortestPathToRow(player2Square, 8).size();
		} else {
			score = shortestPathToRow(player2Square, 8).size()-shortestPathToRow(player1Square, 0).size();
		}
		return score;
	}
	
	/**
	 * Terminal Test
	 * @return true if any player has reached the other side
	 */
	public boolean isOver() {
		return player1Square.getRow() == 0 || player2Square.getRow() == 8;
	}

	protected boolean isTraversal (String move) {
		return isValidSyntax(move) && move.length() == 2;
	}

	protected boolean isValidSyntax (String move) {
		return true;
		/*
		Pattern p = Pattern.compile("[a-i][0-9][hv]?");
		Matcher m = p.matcher(move);
		return m.matches();
		*/
	}

	public boolean isValidTraversal (Square dest) {
		if (dest.equals(currentPlayerPosition()) || dest.equals(otherPlayerPosition())) {
			return false;
		} else if (adjacencyList.get(currentPlayerPosition()).contains(dest)) {
			return true;
		} else if (adjacencyList.get(currentPlayerPosition()).contains(otherPlayerPosition())) {
			if (adjacencyList.get(otherPlayerPosition()).contains(currentPlayerPosition().opposite(otherPlayerPosition()))) {
				return adjacencyList.get(otherPlayerPosition()).contains(dest) && currentPlayerPosition().isCardinalTo(dest);
			} else {
				return adjacencyList.get(otherPlayerPosition()).contains(dest);
			}
		}
		return false;
	}

	public boolean isValidWallPlacement (Wall wall) {
		
		// Check number of walls placed has not been exceeded for any player
		if (!(numWalls1 < 10 && numWalls2 < 10)) {
			return false;
		}
		
		// Check wall is not being placed at border
		if (wall.northWest.getColumn()==8 || wall.northWest.getRow()==8) {
			return false;
		}

		// Check wall is not intersecting existing wall
		if (wall.orientation == Orientation.HORIZONTAL) {
			if (walls.contains(wall) || walls.contains(wall.neighbor(0, 0, Orientation.VERTICAL)) || walls.contains(wall.neighbor(0, -1, Orientation.HORIZONTAL)) || walls.contains(wall.neighbor(0, 1, Orientation.HORIZONTAL))) {
				return false;
			}
		} else {
			if (walls.contains(wall) || walls.contains(wall.neighbor(0, 0, Orientation.HORIZONTAL)) || walls.contains(wall.neighbor(-1, 0, Orientation.VERTICAL)) || walls.contains(wall.neighbor(1, 0, Orientation.VERTICAL))) {
				return false;
			}
		}

		// If the wall does not intersect existing walls, proceed to update the graph
		// to remove associated edges
		if (wall.orientation==Orientation.HORIZONTAL) {
			removeEdge(wall.northWest, wall.northWest.neighbor(1, 0));
			removeEdge(wall.northWest.neighbor(0, 1), wall.northWest.neighbor(1,1));
		} else {
			removeEdge(wall.northWest, wall.northWest.neighbor(0, 1));
			removeEdge(wall.northWest.neighbor(1, 0), wall.northWest.neighbor(1,1));
		}

		// Check if we have removed a path for any player by the
		// placement of a wall
		boolean hasPath = hasPathToGoal();
		if (wall.orientation==Orientation.HORIZONTAL) {
			addEdge(wall.northWest, wall.northWest.neighbor(1, 0));
			addEdge(wall.northWest.neighbor(0, 1), wall.northWest.neighbor(1,1));
		} else {
			addEdge(wall.northWest, wall.northWest.neighbor(0, 1));
			addEdge(wall.northWest.neighbor(1, 0), wall.northWest.neighbor(1,1));
		}
		return hasPath;
	}
	
	protected boolean isWallPlacement (String move) {
		return isValidSyntax(move) && move.length() == 3;
	}
	
	/**
	 * Mutator method for mutating game state. Return false if invalid move.
	 * @param move
	 */
	public boolean move (String move) {
		boolean valid = true;
		valid = !isOver();
		if (isWallPlacement(move)) {
			Wall wall = new Wall(move);
			valid = isValidWallPlacement(wall);
			if (valid) {
				placeWall(wall);
			}
		} else {
			Square sq = new Square(move);
			valid = isValidTraversal(sq);
			if (valid) {
				traverse(sq);
			}
		}
		if (valid) {
			turn++;
		}
		return valid;
	}


	public Square otherPlayerPosition () {
		return currentPlayerPosition().equals(player1Square) ? player2Square : player1Square;
	}
	
	public void placeWall(Wall wall) {
		if (currentPlayer()==0) {
			numWalls1++;
		} else {
			numWalls2++;
		}
		walls.add(wall);
		if (wall.getOrientation() == Orientation.HORIZONTAL) {
			removeEdge(wall.northWest, wall.northWest.neighbor(1, 0));
			removeEdge(wall.northWest.neighbor(0, 1), wall.northWest.neighbor(1,1));
			wallLookup.put(new Square((wall.getNorthWest().getRow()+1)<<1,((wall.getNorthWest().getColumn()+1)<<1)+1), wall.getOrientation());
			wallLookup.put(new Square((wall.getNorthWest().getRow()+1)<<1,((wall.getNorthWest().getColumn()+1)<<1)-1), wall.getOrientation());
		} else {
			removeEdge(wall.northWest, wall.northWest.neighbor(0, 1));
			removeEdge(wall.northWest.neighbor(1, 0), wall.northWest.neighbor(1,1));
			wallLookup.put(new Square(((wall.getNorthWest().getRow()+1)<<1)+1,(wall.getNorthWest().getColumn()+1)<<1), wall.getOrientation());
			wallLookup.put(new Square(((wall.getNorthWest().getRow()+1)<<1)-1,(wall.getNorthWest().getColumn()+1)<<1), wall.getOrientation());
		}
	}
	
	private char player (int i, int j) {
		Square transformedPosition = new Square((i-1)>>1,(j-1)>>1);
		return player1Square.equals(transformedPosition) ? PLAYER_1_ICON : PLAYER_2_ICON;
	}
	
	// TODO Kind of a first world problem, but could make use of Java's unicode support to render a more aesthetic board.
	private String print (int i, int j) {
		StringBuilder sb = new StringBuilder();
		if ((i+j)%2 == 0) {
			if (j%2 == 0) {
				sb.append ("+");
			} else {
				if (hasPlayer(i,j))
					sb.append (" "+player(i,j)+" ");
				else
					sb.append ("   ");	
			}
		} else {
			if (i%2 == 0) {
				if (hasWall(i,j))
					sb.append ("###");
				else
					sb.append ("---");
			} else {
				if (hasWall(i,j))
					sb.append ("#");
				else
					sb.append ("|");		
			}

		}
		if (j == 2*BOARD_SIZE) 
			sb.append ("\n");
		return sb.toString();
	}

	// Need to check preconditions
	public void removeEdge (Square a, Square b) {
		adjacencyList.get(a).remove(b);
		adjacencyList.get(b).remove(a);
	}
	
	public List<Square> shortestPathToWin () {
		if (currentPlayer()==0) {
			return shortestPathToRow(player1Square, 0);
		} else {
			return shortestPathToRow(player2Square, 8);
		}
	}
	
	public List<Square> shortestPathToRow (int row) {
		return shortestPathToRow(currentPlayerPosition(), row);
	}
	
	protected List<Square> shortestPathToRow (Square src, int row) {
		List<Square> path = new LinkedList<Square>();
		Queue <Square> queue = new LinkedList<Square>();
		HashMap <Square,Square> parentNode = new HashMap<Square,Square>();
		// enqueue start configuration onto queue
		queue.add(src);
		// mark start configuration
		parentNode.put(src, null);
		while (!queue.isEmpty()) {
			Square t = queue.poll();
			if (t.getRow() == row) {
				while (!t.equals(src)) {
					path.add(t);
					t = parentNode.get(t);
				}
				Collections.reverse(path);
				return path;
			}
			for (Square e: adjacencyList.get(t)) {
				if (!parentNode.containsKey(e)) {
					parentNode.put(e, t);
					queue.add(e);
				}
			}
		}
		return path;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: "+turn+" | Player to Move: "+currentPlayer()+"\n");
		//sb.append("Valid Moves: "+validMoves()+"\n");
		//sb.append("Shortest Path to next row: "+shortestPath(currentPlayerPosition(),currentPlayerPosition().getRow()+1)+"\n");
		//sb.append("Heuristic: "+heuristic()+"\n");
		sb.append("   ");
		for (char c = 'a' ; c < 'j' ; c++)
			sb.append(c+"   ");
		sb.append("\n");
		for (int i = 0 ; i < 2*BOARD_SIZE+1 ; i++) {
			for (int j = 0 ; j < 2*BOARD_SIZE+1 ; j++) {
				if (j == 0) {		
					if (i%2==0)
						sb.append(" ");
					else
						sb.append((i+1)>>1);
				}
				sb.append (print (i,j));
			}
		}
		return sb.toString();
	}
	
	public void traverse(Square sq) {
		if (currentPlayer()==0) {
			player1Square = sq;
		} else {
			player2Square = sq;
		}
	}

	public Integer turn () {
		return turn;
	}

	public List<String> validMoves() {
		List<String> validMoves = new LinkedList<String>();
		int row = currentPlayerPosition().getRow();
		int column = currentPlayerPosition().getColumn();
		for (int d = -2; d < 3; d++) {
			if (d != 0) { // Vertices are not self-connecting
				if (row+d >= 0 && row+d < BOARD_SIZE) {
					Square sq = new Square(row+d,column);
					if (isValidTraversal(sq)) {
						validMoves.add(sq.toString());
					}
				}
				if (column+d >= 0 && column+d < BOARD_SIZE) {
					Square sq = new Square(row,column+d);
					if (isValidTraversal(sq)) {
						validMoves.add(sq.toString());
					}
				}
			}
		}		
		for (int i = 0; i < BOARD_SIZE ; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				Square sq = new Square(i,j);
				for (Orientation o: Orientation.values()) {
					Wall wall = new Wall(sq, o);
					if (isValidWallPlacement(wall)) {
						validMoves.add(wall.toString());
					}
				}
			}
		}
		return validMoves;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numWalls1;
		result = prime * result + numWalls2;
		result = prime * result
				+ ((player1Square == null) ? 0 : player1Square.hashCode());
		result = prime * result
				+ ((player2Square == null) ? 0 : player2Square.hashCode());
		result = prime * result + turn;
		result = prime * result + ((walls == null) ? 0 : walls.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameState other = (GameState) obj;
		if (numWalls1 != other.numWalls1)
			return false;
		if (numWalls2 != other.numWalls2)
			return false;
		if (player1Square == null) {
			if (other.player1Square != null)
				return false;
		} else if (!player1Square.equals(other.player1Square))
			return false;
		if (player2Square == null) {
			if (other.player2Square != null)
				return false;
		} else if (!player2Square.equals(other.player2Square))
			return false;
		if (turn != other.turn)
			return false;
		if (walls == null) {
			if (other.walls != null)
				return false;
		} else if (!walls.equals(other.walls))
			return false;
		return true;
	}

}
