package info.mackiewicz.tictactoemultiplayer.controller.dto;

import info.mackiewicz.tictactoemultiplayer.model.GamePiece;

public record GameStartDto(
        String gameId,
        GamePiece symbol,
        String turn
) {
}
