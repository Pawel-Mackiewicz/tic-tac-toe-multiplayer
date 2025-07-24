package info.mackiewicz.tictactoemultiplayer.controller;

import info.mackiewicz.tictactoemultiplayer.controller.dto.MakeMoveDto;
import info.mackiewicz.tictactoemultiplayer.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;

    /**
     * Catches messages sent to the "/app/findGame" destination.
     * Extracts the session ID and tells the service to find a match.
     */
    @MessageMapping("/findGame")
    public void findGame(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameService.handleFindGame(sessionId);
    }

    /**
     * Catches messages sent to "/app/game/{gameId}/move".
     * Extracts session ID, game ID, and move data, then passes them to the service.
     */
    @MessageMapping("/game/{gameId}/move")
    public void makeMove(@DestinationVariable String gameId, MakeMoveDto move, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        log.info("gameId: {}, sessionId: {}", gameId, sessionId);
        gameService.handleMove(sessionId, UUID.fromString(gameId), move);
    }
}