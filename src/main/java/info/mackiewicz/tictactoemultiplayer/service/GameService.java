package info.mackiewicz.tictactoemultiplayer.service;

import info.mackiewicz.tictactoemultiplayer.controller.dto.GameStartDto;
import info.mackiewicz.tictactoemultiplayer.model.Game;
import info.mackiewicz.tictactoemultiplayer.model.GamePiece;
import info.mackiewicz.tictactoemultiplayer.model.Player;
import info.mackiewicz.tictactoemultiplayer.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final String USER_QUEUE_PRIVATE_TOPIC = "/queue/private";

    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicReference<String> waitingPlayerSessionId = new AtomicReference<>();


    public void handleFindGame(String sessionId) {
        String waitingPlayerSessionId = this.waitingPlayerSessionId.getAndSet(sessionId);

        if (waitingPlayerSessionId == null) return;

        createAndSaveGame(waitingPlayerSessionId, sessionId);
   }

    private void createAndSaveGame(String firstPlayerSession, String secondPlayerSession) {
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
}
