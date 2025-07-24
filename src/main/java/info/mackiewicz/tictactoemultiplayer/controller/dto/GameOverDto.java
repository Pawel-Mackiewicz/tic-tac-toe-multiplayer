package info.mackiewicz.tictactoemultiplayer.controller.dto;

public record GameOverDto(
        String[][] board,
        String winner
) {
}

