package info.mackiewicz.tictactoemultiplayer.controller.dto;

public record GameStateDto(
        String[][] board,
        String turn
) {
}
