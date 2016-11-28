import java.io.Serializable;


@SuppressWarnings("serial")
public class GameState implements Serializable
{
	public final int numQueens;
	private boolean[] isColumnThreatened;
	private boolean[] isForwardDiagonalThreatened;
	private boolean[] isBackwardDiagonalThreatened;
	
	private Position[] queenPositions;
	
	
	public GameState(int numQueens)
	{
		this.numQueens = numQueens;
		isColumnThreatened = new boolean[numQueens];
		this.queenPositions = new Position[numQueens];
		
		isForwardDiagonalThreatened = new boolean[(numQueens*2) - 1];
		isBackwardDiagonalThreatened = new boolean[(numQueens*2) - 1];
	}
	
	private int forwardPos(int row, int col)
	{
		return row+col;
	}
	
	private int backwardPos(int row, int col)
	{
		return col-row+(numQueens-1);
	}
	
	public boolean isThreatened(int row, int col)
	{
		int forwardDiagonalPos = forwardPos(row, col);
		int backwardDiagonalPos = backwardPos(row, col);
		
		if (isColumnThreatened[col] || 
				isForwardDiagonalThreatened[forwardDiagonalPos] ||
				isBackwardDiagonalThreatened[backwardDiagonalPos]) return true;
		
		return false;
	}
	
	public void putQueen(int queenID, int row, int col)
	{
		// Get diagonal position
		int forwardDiagonalPos = forwardPos(row, col);
		int backwardDiagonalPos = backwardPos(row, col);
		
		// Set arrays to threatened
		isColumnThreatened[col] = true;
		isForwardDiagonalThreatened[forwardDiagonalPos] = true;
		isBackwardDiagonalThreatened[backwardDiagonalPos] = true;
		
		// Store queen position
		queenPositions[queenID] = new Position(row, col);
	}
	
	public Position getQueenPosition(int queenID)
	{
		if (queenPositions.length <= queenID) return null;
		return queenPositions[queenID];
	}
	
	public void removeQueen(int queenID)
	{
		if (queenPositions.length <= queenID) return;
		
		Position pos = queenPositions[queenID];
		if (pos == null) return;
		
		// Get diagonal position
		int forwardDiagonalPos = forwardPos(pos.row, pos.col);
		int backwardDiagonalPos = backwardPos(pos.row, pos.col);
		
		isColumnThreatened[pos.col] = false;
		isForwardDiagonalThreatened[forwardDiagonalPos] = false;
		isBackwardDiagonalThreatened[backwardDiagonalPos] = false;
		
		queenPositions[queenID] = null;
	}
	
	public class Position
	{
		public final int row;
		public final int col;
		
		public Position(int row, int col)
		{
			this.row = row;
			this.col = col;
		}
	}
}
