import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

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
public class TicTacToeServer extends Application implements Constants{
	private int sessionNum = 1;

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		TextArea taLog = new TextArea();
		
		Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
		primaryStage.setTitle("Server Game: TicTacToe");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		new Thread(() -> {
			try {
				ServerSocket serverSocket = new ServerSocket(PORT);
				Platform.runLater(() -> taLog.appendText(new Date() +": server started at socket 8004\n"));
				
				while(true) {
					Platform.runLater(() -> taLog.appendText(new Date() + ": wait for players to join session " + sessionNum + "\n"));
					
					//Connect to player 1
					Socket player1 = serverSocket.accept();
					
					Platform.runLater(() -> {
						taLog.appendText(new Date() + ": player 1 joined session " + sessionNum + "\n");
						taLog.appendText("Player 1's IP adress: " + player1.getInetAddress().getHostAddress() + "\n");
					});
					
					//Notify that the player is Player 1
					new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);
					
					//Connect to player 2
					Socket player2 = serverSocket.accept();
					
					Platform.runLater(() -> {
						taLog.appendText(new Date() + ": player 2 joined session " + sessionNum + "\n");
						taLog.appendText("Player 2's IP adress: " + player2.getInetAddress().getHostAddress() + "\n");
					});
					
					//Notify that the player 2 is Player 2
					new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);
					
					//Display this session and increment session number 
					Platform.runLater(() -> taLog.appendText(new Date() + ": START a thread for session " + sessionNum + "\n"));
					
					//Launch new thread for this session of two player
					new Thread(new HandleASession(player1, player2)).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	class HandleASession implements Runnable, Constants {
		private Socket player1;
		private Socket player2;
		
		private char[][] cell = new char[3][3];
		
		private DataInputStream fromPlayer1;
		private DataOutputStream toPlayer1;
		
		private DataInputStream fromPlayer2;
		private DataOutputStream toPlayer2;
		
		private boolean continueToPlay = true;
		
		public HandleASession(Socket player1, Socket player2) {
			this.player1 = player1;
			this.player2 = player2;
			
			for(int i = 0; i < 3; i++)
				for(int j = 0; j < 3; j++)
					cell[i][j] = ' ';
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				fromPlayer1 = new DataInputStream(player1.getInputStream());
				toPlayer1   = new DataOutputStream(player1.getOutputStream());
				
				fromPlayer2 = new DataInputStream(player2.getInputStream());
				toPlayer2   = new DataOutputStream(player2.getOutputStream());
				
				//Write anything to notify player 1 to start
				//This is just to left player 1 known to start
				toPlayer1.writeInt(1);
				
				//Contionuosly serve the player and determine and report
				// the game status to the player
				while(true) {
					//Receive a move from player 1
					int row = fromPlayer1.readInt();
					int column = fromPlayer1.readInt();
					cell[row][column] = 'X';
					
					//Check if player 1 wins
					if(isWon('X')) {
						toPlayer1.writeInt(PLAYER1_WON);
						toPlayer2.writeInt(PLAYER1_WON);
						sendMove(toPlayer2, row, column);
						break; //Break the loop
					} else if(isFull()) {
						toPlayer1.writeInt(DRAW);
						toPlayer2.writeInt(DRAW);
						sendMove(toPlayer2, row, column);
					} else {
						//Notify player 2 to take turn
						toPlayer2.writeInt(CONTINUE);
						//Send player 1's selected row and column to player 2
						sendMove(toPlayer2, row, column);
					}
					
					//Receive a move from player 2
					row = fromPlayer2.readInt();
					column = fromPlayer2.readInt();
					cell[row][column] = '0';
					
					//Check if player 2 wins
					if(isWon('0')) {
						toPlayer1.writeInt(PLAYER2_WON);
						toPlayer2.writeInt(PLAYER2_WON);
						sendMove(toPlayer1, row, column);
						break;
					} else {
						//Notify player 1 to take the turn
						toPlayer1.writeInt(CONTINUE);
						
						//Send player 2's selected row and column to player 1
						sendMove(toPlayer1, row, column);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		/** Send the move to other player */
		private void sendMove(DataOutputStream out, int row, int column) throws IOException {
			out.writeInt(row);
			out.writeInt(column);
		}
		
		/** Determine if the cells are all occupied */
		private boolean isFull() {
			for(int i = 0; i < 3; i++)
				for(int j = 0; j < 3; j++)
					if(cell[i][j] == ' ')
						return false;//At least one cell is not filled
			
			//All cells are filled
			return true;
		}
		
		/** Determine if the player with the specified token wins */
		private boolean isWon(char token) {
			/** Check all rows */
			for(int i = 0; i < 3; i++)
				if((cell[i][0] == token) && (cell[i][1] == token) && (cell[i][2] == token))
					return true;
			
			/** Checl all columns */
			for(int j = 0; j < 3; j++)
				if((cell[0][j] == token) && (cell[1][j] == token) && (cell[2][j] == token))
					return true;
			
			/** Check major diagonal */
			if((cell[0][0] == token) && (cell[1][1] == token) && (cell[2][0] == token))
				return true;
			
			/** Check major subdiagonal */
			if((cell[0][2] == token) && (cell[1][1] == token) && (cell[2][0]) == token)
				return true;
			
			/** All checked but no winner */
			return false;
			
		}
		
	}
	
	public static void main(String[] args) {
		Application.launch();
	}
}
