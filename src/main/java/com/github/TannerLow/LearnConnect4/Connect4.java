package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaMatrixMath.Matrix;

import java.security.InvalidParameterException;

public class Connect4 {

    enum Outcome { Win, Loss, Tie, Unfinished };

    public final static int boardWidth = 7;
    public final static int boardHeight = 6;
    private Player[][] board = new Player[boardHeight][boardWidth];
    private Player currentPlayer = Player.Player1;
    private Player winner = Player.Neither;
    private static final int[][][] axes = {
            {{ 0,1}, { 0,-1}}, // up & down
            {{ 1,0}, {-1, 0}}, // left & right
            {{ 1,1}, {-1,-1}}, // up-right & down-left
            {{-1,1}, { 1,-1}} // up-left & down-right
    };
    private int turnCount = 0;

    public Connect4() {
        for(int row = 0; row < boardHeight; row++) {
            for(int col = 0; col < boardWidth; col++) {
                board[row][col] = Player.Neither;
            }
        }
    }

    public boolean play(int column) throws InvalidParameterException {
        turnCount++;

        if(column < 0 || column >= boardWidth) {
            throw new InvalidParameterException("Column not in bounds 0-6: " + column);
        }

        if(board[boardHeight - 1][column] != Player.Neither) {
            return false;
        }

        for(int row = 0; row < board.length; row++) {
            if(board[row][column] == Player.Neither) {
                board[row][column] = currentPlayer;
                // check for win condition
                int[] origin = {column, row};
                for(int[][] axis : axes) {
                    int count = countConnected(currentPlayer, origin, axis[0], axis[1]);
                    if(count >= 4) {
                        winner = currentPlayer;
                    }
                }
                break;
            }
        }

        currentPlayer = (currentPlayer == Player.Player1) ? Player.Player2 : Player.Player1;

        return true;
    }

    public Player getWinner() {
        return winner;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Matrix getEncodedGameState(Player player) {
        Matrix matrix = new Matrix(boardWidth * boardHeight * 2 + 1, 1);
        int i = 0;
        for(Player[] row : board) {
            for(Player spot : row) {
                if(spot == Player.Neither) {
                    matrix.data[i*2] = 0;
                    matrix.data[i*2 + 1] = 0;
                }
                else if(spot == player) {
                    matrix.data[i*2] = 1;
                    matrix.data[i*2 + 1] = 0;
                }
                else {
                    matrix.data[i*2] = 0;
                    matrix.data[i*2 + 1] = 1;
                }
                i++;
            }
        }
        matrix.data[i*2] = getNormalizedTurnCount();

        return matrix;
    }

    private float getNormalizedTurnCount() {
        return (float) (1.0 / (1.0 + Math.exp(-0.06 * (turnCount - 42))));
    }

    public boolean isContinuable() {
        if(getWinner() != Player.Neither) {
            return false;
        }

        for(int column = 0; column < boardWidth; column++) {
            if(board[boardHeight - 1][column] == Player.Neither) {
                return true;
            }
        }

        return false;
    }

    private int countConnected(Player player, int[] origin, int[] directionA, int[] directionB) {
        int count1 = 0;
        int count2 = 0;

        int[] position = {origin[0], origin[1]};

        Player currentSpot = player;
        int horizontalDirection = directionA[0];
        int verticalDirection = directionA[1];
        while(currentSpot == player) {
            count1++;
            int column = position[0] + horizontalDirection;
            int row = position[1] + verticalDirection;
            if(column >= 0 && column < boardWidth) {
                if(row >= 0 && row < boardHeight) {
                    currentSpot = board[row][column];
                    position[0] = column;
                    position[1] = row;
                    continue;
                }
            }
            currentSpot = Player.Neither;
        }

        position[0] = origin[0];
        position[1] = origin[1];
        currentSpot = player;
        horizontalDirection = directionB[0];
        verticalDirection = directionB[1];
        while(currentSpot == player) {
            count2++;
            int column = position[0] + horizontalDirection;
            int row = position[1] + verticalDirection;
            if(column >= 0 && column < boardWidth) {
                if(row >= 0 && row < boardHeight) {
                    currentSpot = board[row][column];
                    position[0] = column;
                    position[1] = row;
                    continue;
                }
            }
            currentSpot = Player.Neither;
        }

        return count1 + count2 - 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int row = boardHeight-1; row >= 0; row--) {
            for(int col = 0; col < boardWidth; col++) {
                sb.append(board[row][col].ordinal());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
