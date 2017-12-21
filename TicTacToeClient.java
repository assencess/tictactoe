import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.application.*;
import javafx.geometry.*;
import javafx.stage.Stage;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.paint.*;
import javafx.event.*;

@SuppressWarnings("restriction")
public class TicTacToeClient extends Application implements Constants{
	//Indicator whether the player has the turn
	private boolean myTurn = false;
	
	//Indicate the token for the player
	private char myToken = ' ';
	
	//Indicate the token for the other player
	private char otherToken = ' ';
	
	//Create and initialize cells
	private Cell[][] cell = new Cell[3][3];
	
	//Create and initialize a title label
	private Label lblTitle = new Label();
	
	//Create and initialize a status bar label
	private Label lblStatus = new Label();
	
	//Indicate selected row and column by the current move
	private int rowSelected;
	private int columnSelected;
	
	//Input and Output streams from/to server
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	
	//Continue to play?
	private boolean continueToPlay = true;
	
	//Wait for the player mark a cell
	private boolean waiting = true;
	
	/** Start method of application */
	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		GridPane pane = new GridPane();
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 3; j++)
				pane.add(cell[i][j] = new Cell(i, j), j, i);
		
		BorderPane borderPane = new BorderPane();
		borderPane.setTop(lblTitle);
		borderPane.setCenter(pane);
		borderPane.setBottom(lblStatus);
		
		//Create a scene and place it in the stage
		Scene scene = new Scene(borderPane, 320, 350);
		primaryStage.setTitle("TicTacToeOnline Clien");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		//Connect to server
		connectToServer();
	}
	
	private void connectToServer() {
		try {
			// Create a socket to connect to the server
			Socket socket = new Socket(HOST, PORT);
			
			//Create an input stream to receive data from the server
			fromServer = new DataInputStream(socket.getInputStream());
			
			//Create an output stream to send date to the server
			toServer = new DataOutputStream(socket.getOutputStream());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//Control a game on a separate thread
		new Thread(() -> {
			try {
				//Get notification from the server
				int player = fromServer.readInt();
				
				//Am I player 1 or 2?
				if(player == PLAYER1) {
					myToken = 'X';
					otherToken = '0';
					Platform.runLater(() -> {
						lblTitle.setText("Player 1 with token 'X'");
						lblStatus.setText("Waiting for player 2 to join");
					});
					
					//Receive startup notification from the server
					fromServer.readInt(); //Whatever read is ignored
					
					//The other player has joined
					Platform.runLater(() -> lblStatus.setText("Player 2 is joined. I start first"));
					
					//It is my turn
					myTurn = true;
				} else if(player == PLAYER2) {
					myToken = '0';
					otherToken = 'X';
					Platform.runLater(() -> {
						lblTitle.setText("Player 2 with token '0'");
						lblStatus.setText("Waiting for player 1 to move");
					});
					
				}
				
				//Continue to play
				while(continueToPlay) {
					if(player == PLAYER1) {
						waitForPlayerAction(); //Wait for player 1 to move
						sendMove();      	   //Send the move to the server 
						receiveInfoFromServer(); // Receive into from the Server
					} else if(player == PLAYER2) {
						receiveInfoFromServer(); //Receive info from the server
						waitForPlayerAction();   //Wait for player 2 to move
						sendMove();				 //Send player 2's move to the server
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
	}
	
	/** Wait for the player to mark a cell */
	private void waitForPlayerAction() throws InterruptedException {
		while(waiting) {
			Thread.sleep(100);
		}
		waiting = true;
	}
	
	/**Send this player's move to the Server */
	private void sendMove() throws IOException {
		toServer.writeInt(rowSelected);    //Send the selected row
		toServer.writeInt(columnSelected); //Send the selected column
	}
	
	/** Receive onfo from the server */
	private void receiveInfoFromServer() throws IOException{
		//Receive game status 
		int status = fromServer.readInt();
		
		if(status == PLAYER1_WON) {
			//Player 1 won, stop playing
			continueToPlay = false;
			if(myToken == 'X')
				Platform.runLater(() -> lblStatus.setText("I won! (X)"));
			else if(myToken == '0') {
				Platform.runLater(() -> lblStatus.setText("Player 1 (X) has won!"));
				receiveMove();
			}
		} else if(status == PLAYER2_WON) {
			//Player 2 won, stop playing
			continueToPlay = false;
			if(myToken == '0')
				Platform.runLater(() -> lblStatus.setText("I won! (0)"));
			else if(myToken == 'X') {
				Platform.runLater(() -> lblStatus.setText("Player 2 (0) has won!"));
				receiveMove();
			}
		} else if(status == DRAW) {
			// No winner, game is over
			continueToPlay = false;
			Platform.runLater(() -> lblStatus.setText("Game is over, no winner!"));
			
			if(myToken == '0') {
				receiveMove();
			}
		} else {
			receiveMove();
			Platform.runLater(() -> lblStatus.setText("My Turn!"));
			myTurn = true; // It's my time!
		}
	}
	
	private void receiveMove() throws IOException {
		//Get the other player's move
		int row = fromServer.readInt();
		int column = fromServer.readInt();
		
		System.out.println("Method : receiveMove();");
		
		Platform.runLater(() -> cell[row][column].setToken(otherToken));
	}
	
	public static void main(String[] args) {
		Application.launch();
	}

	
	class Cell extends Pane { 
		//Indicate the row and column of this cell in the board
		private int row;
		private int column;
		
		//This used for this cell
		private char token = ' ';
		
		public Cell(int row, int column) {
			this.row = row;
			this.column = column;
			
			setStyle("-fx-border-color: black;");
			this.setPrefSize(2000, 2000);
			this.setOnMouseClicked(e -> handleMouse());
		}
		
		public char getToken() {
			return token;
		}
		
		public void setToken(char c) {
			token = c;
			repaint();
		}
		
		private void repaint() {
			if(token == 'X') {
				Line line1 = new Line(10, 10, this.getWidth() - 10, this.getHeight() -10);
				line1.endXProperty().bind(this.widthProperty().subtract(10));
				line1.endYProperty().bind(this.heightProperty().subtract(10));
				
				Line line2 = new Line(10, this.getHeight() - 10, this.getWidth() - 10, 10);
				line2.startYProperty().bind(this.heightProperty().subtract(10));
				line2.endXProperty().bind(this.widthProperty().subtract(10));
				
				this.getChildren().addAll(line1, line2);
			} else if(token == '0') {
				Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2, this.getWidth() / 2 - 10, this.getHeight() / 2 - 10);
				ellipse.centerXProperty().bind(this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(this.heightProperty().divide(2));
				
				ellipse.radiusXProperty().bind(this.widthProperty().divide(2).subtract(10));
				ellipse.radiusYProperty().bind(this.heightProperty().divide(2).subtract(10));
				
				ellipse.setStroke(Color.BLACK);
				ellipse.setFill(Color.WHITE);
				
				getChildren().add(ellipse);
			}
		}
		
		/** Handle mouse click event */
		private void handleMouse() {
			// If cell is not occupied and the player has the turn
			if(token == ' ' && myTurn) {
				setToken(myToken);
				myTurn = false;
				rowSelected = row;
				columnSelected = column;
				lblStatus.setText("Waiting for the other player to move.");
				waiting = false;
			}
		}
	}
}
