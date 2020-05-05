package assignment7;
/*
 * Author: Vallath Nandakumar and EE 422C instructors
 * Date: April 20, 2020
 * This starter code is from the MultiThreadChat example from the lecture, and is on Canvas. 
 * It doesn't compile.
 */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;


import java.awt.event.TextEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;


public class Client extends Application{
	// I/O streams 
	ObjectOutputStream toServer = null; 
	ObjectInputStream fromServer = null;
	@FXML private Label currentBid;
	@FXML private Label currentWinner;
	@FXML private Label feedback;
	@FXML private TextField bidAmountField;
	@FXML private ChoiceBox currentItemDropdown;
	private static FXMLLoader loader;
	private static String clientID;
	private static boolean waitingForFeedback = false;
	private static boolean successfulBid = false;
	private static ArrayList<Bid> newBids = new ArrayList<>();
	private static ArrayList<Item> items = new ArrayList<>();
	private String currentItem = null;
	@FXML
	public void placeBid(){
		if(currentItem==null){return;}
		feedback.setText("");
		try{
			Double bid = Double.valueOf(bidAmountField.getText());
			newBids.add(new Bid(clientID,bid,currentItem));
			waitingForFeedback=true;
			while(waitingForFeedback){Thread.sleep(100);}
			if(successfulBid){
				feedback.setText("Bid placed. You are the current winner!");
			}
			else {feedback.setText("Invalid Bid. Please try again!");}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	@FXML
	public void changeCurrentItem(){
		if(currentItemDropdown.getSelectionModel().isEmpty()){return;}
		currentItem = (String) currentItemDropdown.getValue();
		for(Item i:items){
			if (i.getID().equals(currentItem)) {
				currentWinner.setText(i.getOwner());
				currentBid.setText(String.valueOf(i.getCurrentBid()));
			}
		}
	}
	@Override
	public void start(Stage primaryStage) {
		try {
			// Create a socket to connect to the server
			@SuppressWarnings("resource")
			Socket socket = new Socket("192.168.1.125", 5000);
			// Create an input stream to receive data from the server
			fromServer = new ObjectInputStream(socket.getInputStream());
			// Create an output stream to send data to the server
			toServer = new ObjectOutputStream(socket.getOutputStream());

			items = ((Auction)fromServer.readObject()).getAuctionItems();
			// Create a scene and place it in the stage
			Pane startPane = new Pane();
			startPane.setPrefSize(600, 400);
			Button startButton = new Button("Login");
			startButton.setLayoutX(77);
			startButton.setLayoutY(150);
			startButton.setTextAlignment(TextAlignment.CENTER);
			startButton.setFont(Font.font("Arial Black", 12));
			TextField username = new TextField("username");
			TextField password = new TextField("password");
			username.setLayoutX(77);
			password.setLayoutX(77);
			username.setLayoutY(50);
			password.setLayoutY(100);
			startButton.setOnAction(event -> {
				try {
					clientID=username.getText();
					loader=new FXMLLoader();
					loader.setLocation(getClass().getResource("clientWindow.fxml"));
					primaryStage.setScene(new Scene(loader.load(),1200,600)); // Place the scene in the stage
					ObservableList<String> options = FXCollections.observableArrayList();
					for(Item i:items){
						options.add(i.getID());
					}
					((Client)loader.getController()).setDropdown(options);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			startPane.getChildren().add(username);
			startPane.getChildren().add(password);
			startPane.getChildren().add(startButton);
			primaryStage.setScene(new Scene(startPane, 300,200));
			primaryStage.setTitle("Client"); // Set the stage title
			primaryStage.show(); // Display the stage
		}
		catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		Thread writerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					if(Client.newBids.size()>0){
						try {
							System.out.println("sending...");
							toServer.writeObject(Client.newBids.remove(0));
							toServer.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		Thread readerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Object input;
				while (true) {
					try {
						input = fromServer.readObject();
						//System.out.println("From server: " + input);
						if (input.equals(clientID+" success")) {
							successfulBid=true;
							setWaitingForFeedback(false);
						} else if(input.equals(clientID+" failed")){
							successfulBid=false;
							setWaitingForFeedback(false);
						} else {
							if(input instanceof Bid) {
								Bid newBid = (Bid) input;
								for (Item i : items) {
									if (newBid.getItemID().equals(i.ID)) {
										i.setCurrentBid(newBid.getBid());
										i.setOwner(newBid.getClientID());
										Platform.runLater(new Runnable(){
											@Override
											public void run(){
											((Client)loader.getController()).updateScreen(i);
											}
										});
										//System.out.println(i.getCurrentBid());
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		writerThread.start();
		readerThread.start();

	}
	public void setDropdown(ObservableList e){
		currentItemDropdown.getItems().addAll(e);
	}
	private void updateScreen(Item item){
		currentBid.setText(String.valueOf(item.getCurrentBid()));
		currentWinner.setText(item.getOwner());
	}
	private void setWaitingForFeedback(boolean value){waitingForFeedback=value;}
	public static void main(String[] args) {
		launch(args);
	}
}
