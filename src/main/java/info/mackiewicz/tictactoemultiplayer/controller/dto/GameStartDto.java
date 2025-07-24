package info.mackiewicz.tictactoemultiplayer.controller.dto;

import info.mackiewicz.tictactoemultiplayer.model.GamePiece;

import java.util.UUID;

public record GameStartDto(
        UUID gameId,
        GamePiece symbol,
        GamePiece turn
) {
}
