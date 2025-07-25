package info.mackiewicz.tictactoemultiplayer.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Game {
    private final UUID gameId;
    private final Player playerX;
    private final Player playerO; //(this isnt 0/zero, it represents circle in tic-tac-toe)
    private GamePiece[][] board;
    private GamePiece currentTurn; //X or O (this isnt 0/zero)
    private GameStatus status;
    private GamePiece winner; //"X", "O", null if draw
    private boolean isDraw;


    public Game(UUID gameId, Player playerX, Player playerO) {
        this.gameId = gameId;
        this.playerX = playerX;
        this.playerO = playerO;
        this.board = new GamePiece[3][3]; // Initializes with nulls
        this.currentTurn = GamePiece.X; // X always starts
        this.status = GameStatus.IN_PROGRESS;
        this.isDraw = false;
    }
}
