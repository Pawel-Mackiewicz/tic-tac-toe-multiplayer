# tic-tac-toe-multiplayer

## Infos about connecting FE to BE
Of course. This is a crucial step. The frontend developer needs a clear, unambiguous contract. Here is the complete API documentation for your Tic-Tac-Toe frontend.

---

## **Tic-Tac-Toe Game API Specification**

This document outlines the full communication protocol between the client (browser) and the server for the multiplayer Tic-Tac-Toe game. The communication is handled via WebSockets using the **STOMP** protocol.

### **1. Connection Setup**

First, the client must establish a WebSocket connection and initialize a STOMP client over it.

*   **WebSocket Endpoint:** `ws://<your-server-address>/ws`
*   **Recommended Libraries:** `sockjs-client` and `stompjs` (or `@stomp/stompjs`) for JavaScript.

**Example JS Connection:**

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const socket = new SockJS('http://<your-server-address>/ws'); // Note: SockJS uses http/https
const stompClient = new Client({
  webSocketFactory: () => socket
});

stompClient.onConnect = (frame) => {
  console.log('Connected: ' + frame);
  // Once connected, subscribe to topics and you are ready to send messages.
};

stompClient.activate();
```

---

### **2. Client-to-Server Messages (Destinations)**

These are the destinations the client will send messages **to**. The standard STOMP prefix for these messages is `/app`.

#### **A. Find a Game**

*   **STOMP Destination:** `/app/findGame`
*   **Description:** Sent after a successful connection to request matchmaking. The server will place the player in a waiting queue. If another player is waiting, a game will start.
*   **Payload:** None. The act of sending to this destination is the request.

**Example JS:**

```javascript
stompClient.publish({
  destination: '/app/findGame'
});
```

#### **B. Make a Move**

*   **STOMP Destination:** `/app/game/{gameId}/move`
*   **Description:** Sent when a player clicks a square on the board to make their move. The `{gameId}` must be replaced with the actual ID of the game received from the `GAME_START` message.
*   **Payload:** A JSON object specifying the move coordinates.

**Payload Structure (`MakeMoveDto`):**

```json
{
  "row": 1,
  "col": 2
}
```
*   `row`: `integer` (0, 1, or 2)
*   `col`: `integer` (0, 1, or 2)

**Example JS:**

```javascript
const gameId = '...'; // The ID you received when the game started
stompClient.publish({
  destination: `/app/game/${gameId}/move`,
  body: JSON.stringify({ row: 1, col: 2 })
});
```

---

### **3. Server-to-Client Messages (Subscriptions)**

These are the topics the client must **subscribe to** in order to receive updates from the server.

#### **A. Private User Channel**

This channel delivers messages intended only for the specific, individual user.

*   **Subscription Topic:** `/user/queue/private`
*   **Description:** Subscribe to this immediately after connecting. It's used for personalized events like the game starting or receiving an error message.

**Messages Received on this Topic:**

1.  **`GAME_START`**
    *   **Description:** Informs the client a match has been found and the game is starting. **Crucially, this message contains the `gameId` needed for all future communication.** After receiving this, the client **must** subscribe to the public game topic (`/topic/game/{gameId}`).
    *   **Payload (`GameStartDto`):**
        ```json
        {
          "gameId": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
          "symbol": "X",
          "turn": "X"
        }
        ```
        *   `gameId`: `string` - The unique ID for this match.
        *   `symbol`: `string` - The piece you will play as (`"X"` or `"O"`).
        *   `turn`: `string` - The piece whose turn it is first (`"X"` or `"O"`). Use `symbol === turn` to determine if it's your move.

2.  **`ERROR`**
    *   **Description:** Sent if the client attempts an illegal action (e.g., moving out of turn).
    *   **Payload (`ErrorDto`):**
        ```json
        {
          "message": "Not your turn."
        }
        ```

#### **B. Public Game Channel**

This channel broadcasts public events to everyone in a specific game (i.e., both players).

*   **Subscription Topic:** `/topic/game/{gameId}`
*   **Description:** Subscribe to this **after** receiving the `GAME_START` message. The `{gameId}` must be replaced with the ID from that message.

**Messages Received on this Topic:**

1.  **`GAME_STATE`**
    *   **Description:** Sent to both players after every valid move to provide the new state of the board.
    *   **Payload (`GameStateDto`):**
        ```json
        {
          "board": [
            [null, "X", null],
            ["O", null, null],
            [null, null, null]
          ],
          "turn": "O"
        }
        ```
        *   `board`: A 3x3 array of `string` or `null`. Can be `"X"`, `"O"`, or `null`.
        *   `turn`: `string` - The piece whose turn it is now.

2.  **`GAME_OVER`**
    *   **Description:** Sent to both players when the game has concluded.
    *   **Payload (`GameOverDto`):**
        ```json
        {
          "board": [
            ["X", "X", "X"],
            ["O", "O", null],
            [null, null, null]
          ],
          "winner": "X"
        }
        ```
        *   `board`: The final 3x3 board state.
        *   `winner`: `string` - The winning piece (`"X"` or `"O"`) or `"DRAW"` for a tie.

---

### **4. Complete Game Flow Example**

1.  **Player A Connects:**
    *   Establishes WebSocket/STOMP connection.
    *   `SUBSCRIBE` to `/user/queue/private`.
    *   `SEND` to `/app/findGame`.
    *   *Client waits.*

2.  **Player B Connects:**
    *   Establishes WebSocket/STOMP connection.
    *   `SUBSCRIBE` to `/user/queue/private`.
    *   `SEND` to `/app/findGame`.

3.  **Game Starts:**
    *   **Server -> Player A** (on `/user/queue/private`): `GAME_START` `{ "gameId": "g-123", "symbol": "X", "turn": "X" }`
    *   **Server -> Player B** (on `/user/queue/private`): `GAME_START` `{ "gameId": "g-123", "symbol": "O", "turn": "X" }`
    *   Player A's client sees it's their turn.
    *   **Both clients now `SUBSCRIBE` to `/topic/game/g-123`**.

4.  **Gameplay:**
    *   **Player A -> Server:** `SEND` to `/app/game/g-123/move` with body `{ "row": 0, "col": 0 }`.
    *   **Server -> Both Players** (on `/topic/game/g-123`): `GAME_STATE` `{ "board": [["X",...]], "turn": "O" }`.
    *   Player B's client sees it's their turn.
    *   **Player B -> Server:** `SEND` to `/app/game/g-123/move` with body `{ "row": 1, "col": 1 }`.
    *   **Server -> Both Players** (on `/topic/game/g-123`): `GAME_STATE` `{ "board": [["X",...], [null,"O",...]], "turn": "X" }`.

5.  **Game Ends:**
    *   Player A makes a winning move.
    *   **Server -> Both Players** (on `/topic/game/g-123`): `GAME_OVER` `{ "board": [...], "winner": "X" }`.
    *   Clients display the result and can disable the board. The game is over.