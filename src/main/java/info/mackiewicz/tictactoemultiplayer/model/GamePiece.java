package info.mackiewicz.tictactoemultiplayer.model;

public enum GamePiece {
    X("X"),
    O("O");

    private String displayName;

    GamePiece(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return this.displayName;
    }
}
