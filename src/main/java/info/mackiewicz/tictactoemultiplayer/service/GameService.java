package info.mackiewicz.tictactoemultiplayer.service;

import info.mackiewicz.tictactoemultiplayer.controller.dto.*;
import info.mackiewicz.tictactoemultiplayer.model.Game;
import info.mackiewicz.tictactoemultiplayer.model.GamePiece;
import info.mackiewicz.tictactoemultiplayer.model.GameStatus;
import info.mackiewicz.tictactoemultiplayer.model.Player;
import info.mackiewicz.tictactoemultiplayer.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
//TODO: ADD LOGS WHEN FINISHED
public class GameService {

    private static final String USER_QUEUE_PRIVATE_TOPIC = "/queue/private";
    private static final String GAME_TOPIC = "/game/";

    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicReference<String> waitingPlayerSessionId = new AtomicReference<>();

    public void handleFindGame(String sessionId) {
        // Try to atomically swap the waiting player with null.
        String opponentSessionId = this.waitingPlayerSessionId.getAndSet(null);

        // Case 1: The slot was empty when we checked.
        if (opponentSessionId == null) {
            this.waitingPlayerSessionId.set(sessionId);
            return; // We are now waiting.
        }

        // Case 2: We found an opponent!
        createAndStartGame(opponentSessionId, sessionId);
    }

   public void handleMove(String sessionId, UUID gameId, MakeMoveDto move) {
        Optional<Game> gameOpt = gameRepository.findBySessionId(sessionId);

       if (gameOpt.isEmpty()) {
           log.error("Invalid move: Game not found for session {}", sessionId);
           return;
       }

       Game game = gameOpt.get();

       if (game.getStatus() != GameStatus.IN_PROGRESS) {
           log.warn("Invalid move: Game {} is not in progress.", gameId);
           return;
       }

        Player currentPlayer;
        Player otherPlayer;
        if (game.getPlayerX().sessionId().equals(sessionId)) {
            currentPlayer = game.getPlayerX();
            otherPlayer = game.getPlayerO();
        } else {
            currentPlayer = game.getPlayerO();
            otherPlayer = game.getPlayerX();
        }
        if (currentPlayer.symbol() != game.getCurrentTurn()) {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    USER_QUEUE_PRIVATE_TOPIC,
                    new ErrorDto("Not your turn!")
            );
            return;
        }
        if (game.getBoard()[move.row()][move.col()] != null) {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    USER_QUEUE_PRIVATE_TOPIC,
                    new ErrorDto("Wrong move! This cell is already occupied.")
            );
            return;
        }
        log.info("All validation passed! Move valid.");
        //Getting game board and putting player symbol
        GamePiece[][] gameBoard = game.getBoard();
        gameBoard[move.row()][move.col()] = currentPlayer.symbol();

        Optional<GamePiece> winner = findWinner(gameBoard);
        if (winner.isPresent()) {
            endGame(game, winner.get());
            return;
        }
        if (isBoardFull(gameBoard)) {
            endGame(game, "DRAW");
            return;
        }
        game.setCurrentTurn(otherPlayer.symbol());
        gameRepository.save(game);

       publishGameState(game);
   }



    public void handleDisconnect(String sessionId) {
        //if the disconnecting player was the one waiting, clear the waiting spot
        if (waitingPlayerSessionId.compareAndSet(sessionId, null)) return;

        if (gameRepository.findBySessionId(sessionId).isEmpty()) return;

        Game game = gameRepository.findBySessionId(sessionId).get();

        //nothing to do if game wasn't `IN_PROGRESS`
        if (game.getStatus() != GameStatus.IN_PROGRESS) return;

        Player winner;
        if (game.getPlayerX().sessionId().equals(sessionId)) {
            winner = game.getPlayerO();
        } else {
            winner = game.getPlayerX();
        }
        game.setWinner(winner.symbol());

        endGame(game, game.getWinner());
   }

    private void createAndStartGame(String firstPlayerSession, String secondPlayerSession) {
        Random random = new Random();
        UUID gameId = UUID.randomUUID();
        Player firstPlayer;
        Player secondPlayer;
        if (random.nextBoolean()) {
            firstPlayer = new Player(firstPlayerSession, GamePiece.O);
            secondPlayer = new Player(secondPlayerSession, GamePiece.X);
        } else {
            firstPlayer = new Player(firstPlayerSession, GamePiece.X);
            secondPlayer = new Player(secondPlayerSession, GamePiece.O);
        }
        var newGame = new Game(gameId, secondPlayer, firstPlayer);

        gameRepository.save(newGame);

        GameStartDto firstPlayerPayload = new GameStartDto(
                newGame.getGameId(),
                firstPlayer.symbol(),
                newGame.getCurrentTurn()
        );

        GameStartDto secondPlayerPayload = new GameStartDto(
                newGame.getGameId(),
                secondPlayer.symbol(),
                newGame.getCurrentTurn()
        );
        messagingTemplate.convertAndSendToUser(firstPlayerSession, USER_QUEUE_PRIVATE_TOPIC, firstPlayerPayload);
        messagingTemplate.convertAndSendToUser(secondPlayerSession, USER_QUEUE_PRIVATE_TOPIC, secondPlayerPayload);
    }

    private void endGame(Game game, GamePiece winner) {
       game.setStatus(GameStatus.FINISHED);
       game.setWinner(winner);

        publishGameOver(game);

        gameRepository.remove(game.getGameId());
    }

    private void endGame(Game game, String winner) {
        game.setStatus(GameStatus.FINISHED);
        if (winner.equalsIgnoreCase("draw")) {
            game.setDraw(true);
        }
    }

    private Optional<GamePiece> findWinner(GamePiece[][] board) {
        // Check all 3 rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != null && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                return Optional.of(board[i][0]);
            }
        }

        // Check all 3 columns
        for (int i = 0; i < 3; i++) {
            if (board[0][i] != null && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                return Optional.of(board[0][i]);
            }
        }

        // Check the two diagonals
        if (board[0][0] != null && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return Optional.of(board[0][0]);
        }
        if (board[0][2] != null && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return Optional.of(board[0][2]);
        }

        // No winner found
        return Optional.empty();
    }

    /**
     * Checks if the game board is completely filled with pieces.
     *
     * @param board The 3x3 game board.
     * @return true if there are no null cells left, false otherwise.
     */
    private boolean isBoardFull(GamePiece[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == null) {
                    // Found an empty cell, so the board is not full
                    return false;
                }
            }
        }
        // No empty cells were found
        return true;
    }

    private void publishGameState(Game game) {
        var payload = new GameStateDto(game.getBoard(), game.getCurrentTurn());

        messagingTemplate.convertAndSend(GAME_TOPIC + game.getGameId().toString(), payload);
    }

    private void publishGameOver(Game game) {
        var payload = new GameOverDto(game.getBoard(), game.getWinner().toString());
        messagingTemplate.convertAndSend(GAME_TOPIC + game.getGameId().toString(), payload);
    }
}
