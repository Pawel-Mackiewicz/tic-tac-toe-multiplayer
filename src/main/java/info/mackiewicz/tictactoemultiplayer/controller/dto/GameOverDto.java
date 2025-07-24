package info.mackiewicz.tictactoemultiplayer.controller.dto;

import info.mackiewicz.tictactoemultiplayer.model.GamePiece;

public record GameOverDto(
        GamePiece[][] board,
        String winner
) {
}

