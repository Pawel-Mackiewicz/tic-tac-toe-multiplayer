package info.mackiewicz.tictactoemultiplayer.controller.dto;

import info.mackiewicz.tictactoemultiplayer.model.GamePiece;

public record GameStateDto(
        GamePiece[][] board,
        GamePiece turn
) {
}
