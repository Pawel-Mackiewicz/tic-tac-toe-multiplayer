package info.mackiewicz.tictactoemultiplayer.controller.dto;

import info.mackiewicz.tictactoemultiplayer.model.GamePiece;

import java.util.UUID;

public record GameStartDto(
        String type,
        UUID gameId,
        GamePiece symbol,
        GamePiece turn
) {
    public GameStartDto(UUID gameId, GamePiece symbol, GamePiece turn) {
        this("GAME_START", gameId, symbol, turn);
    }
}
