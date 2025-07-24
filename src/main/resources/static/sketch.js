let socket, client;
let subPrivate, subGame;
let gameId, mySymbol, currentTurn;
let board = Array(9).fill(null);
let gameOver = false, resultText = '';
let movePending = false;
let audioResumed = false;

function setup() {
  createCanvas(300, 300);
  initNetworking();
}

function draw() {
  background(255);
  drawBoard();
  drawMarks();

  if (gameOver) {
    noLoop();  
    textSize(32);
    textAlign(CENTER, CENTER);
    fill(0);
    text(resultText, width/2, height/2);
  }
}

function resumeAudioOnce() {
  if (!audioResumed) {
    const ac = getAudioContext();
    if (ac.state !== 'running') ac.resume();
    audioResumed = true;
  }
}

function mousePressed() {
  resumeAudioOnce();

  if (gameOver || currentTurn !== mySymbol || movePending) return;

  const i = floor(mouseX / (width/3));
  const j = floor(mouseY / (height/3));
  const idx = i + j*3;
  if (board[idx] !== null) return;

  movePending = true;
  console.log("gameId: ", gameId);
  const destination = "/app/game/" + gameId + "/move";
  client.publish({
    destination: destination,
    body: JSON.stringify({ row: j, col: i })
  });
}

function keyPressed() {
  if ((key === 'r' || key === 'R') && gameOver) {
    resetGame();
    loop();  
    client.publish({ destination: '/app/findGame' });
  }
}

  function initNetworking() {
    // Directly configure the client with the WebSocket URL
    client = new StompJs.Client({
      brokerURL: 'ws://localhost:8080/ws', // <-- USE THIS INSTEAD OF webSocketFactory
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: msg => console.log('[STOMP]', msg),

    onConnect: () => {
      console.log('STOMP connected');
      subPrivate = client.subscribe('/user/queue/private', onPrivateMessage);
      client.publish({ destination: '/app/findGame' });
    },

    onStompError: frame => {
      console.error('Broker error:', frame.headers['message']);
      console.error('Details:', frame.body);
    }
  });

  client.activate();
}

function onPrivateMessage(msg) {
  const data = JSON.parse(msg.body);

  if (data.type === 'GAME_START') {
    gameId      = data.gameId;
    mySymbol    = data.symbol;
    currentTurn = data.turn;

    if (subGame) subGame.unsubscribe();
    subGame = client.subscribe(
      `/topic/game/${gameId}`,
      m => handlePublic(JSON.parse(m.body))
    );
  }
  else if (data.type === 'ERROR') {
    console.error('Server error:', data.message);
  }
}

function handlePublic(payload) {
  if (payload.type === 'GAME_STATE') {
    board       = payload.board.flat();
    currentTurn = payload.turn;
    movePending = false;
  }
  else if (payload.type === 'GAME_OVER') {
    board      = payload.board.flat();
    gameOver   = true;
    resultText = payload.winner === 'DRAW'
      ? 'Draw!'
      : `${payload.winner} wins!`;
  }
}

function resetGame() {
  board       = Array(9).fill(null);
  currentTurn = null;
  gameOver    = false;
  resultText  = '';
  movePending = false;

  if (subGame)   subGame.unsubscribe();
  if (subPrivate) subPrivate.unsubscribe();
}

function drawBoard() {
  strokeWeight(4);
  const w = width/3, h = height/3;
  line(w, 0, w, height);
  line(2*w, 0, 2*w, height);
  line(0, h, width, h);
  line(0, 2*h, width, 2*h);
}

function drawMarks() {
  textSize(64);
  textAlign(CENTER, CENTER);
  const w = width/3, h = height/3;
  for (let j = 0; j < 3; j++) {
    for (let i = 0; i < 3; i++) {
      const spot = board[i + j*3];
      if (spot) {
        fill(0);
        text(spot, i*w + w/2, j*h + h/2);
      }
    }
  }
}