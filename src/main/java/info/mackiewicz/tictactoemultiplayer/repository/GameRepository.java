package info.mackiewicz.tictactoemultiplayer.repository;

import info.mackiewicz.tictactoemultiplayer.model.Game;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GameRepository {
    private final Map<String, Game> gamesById = new ConcurrentHashMap<>();
    private final Map<String, String> sessionsToGameId = new ConcurrentHashMap<>();

    public void save(Game game) {
        gamesById.put(game.getGameId(), game);
        sessionsToGameId.put(game.getPlayerO().sessionId(), game.getGameId());
        sessionsToGameId.put(game.getPlayerX().sessionId(), game.getGameId());
    }

    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(gamesById.get(gameId));
    }

    public Optional<Game> findBySessionId(String sessionId) {
        String gameId = sessionsToGameId.get(sessionId);
        return findById(gameId);
    }

    public void remove(String gameId) {
        var game = gamesById.get(gameId);

        gamesById.remove(gameId);

        sessionsToGameId.remove(game.getPlayerX().sessionId());
        sessionsToGameId.remove(game.getPlayerO().sessionId());
    }
}
