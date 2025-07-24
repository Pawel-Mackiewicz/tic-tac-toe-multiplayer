package info.mackiewicz.tictactoemultiplayer.repository;

import info.mackiewicz.tictactoemultiplayer.model.Game;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GameRepository {
    private final Map<UUID, Game> gamesById = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionsToGameId = new ConcurrentHashMap<>();

    public Game save(Game game) {
        gamesById.put(game.getGameId(), game);
        sessionsToGameId.put(game.getPlayerO().sessionId(), game.getGameId());
        sessionsToGameId.put(game.getPlayerX().sessionId(), game.getGameId());

        return game;
    }

    public Optional<Game> findById(UUID gameId) {
        return Optional.ofNullable(gamesById.get(gameId));
    }

    public Optional<Game> findBySessionId(String sessionId) {
        UUID gameId = sessionsToGameId.get(sessionId);
        return findById(gameId);
    }

    public void remove(UUID gameId) {
        if (gameId == null) return;

        var game = gamesById.get(gameId);

        gamesById.remove(gameId);

        sessionsToGameId.remove(game.getPlayerX().sessionId());
        sessionsToGameId.remove(game.getPlayerO().sessionId());
    }
}
