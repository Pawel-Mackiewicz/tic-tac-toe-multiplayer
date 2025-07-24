package info.mackiewicz.tictactoemultiplayer.controller.dto;

public record ErrorDto(
        String type,
        String message
) {
    public ErrorDto(String message) {
        this("ERROR", message);
    }
}
