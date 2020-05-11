package assignment7;
/**  EE422C Final Project submission by
 * Jeffrey Wallace
 * jtw2992
 * 16310
 * Spring 2020
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


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	@FXML private Label itemDescriptionLabel;
	@FXML private Label timeRemainingLabel;
	private static FXMLLoader loader;
	private static String clientID;
	private static boolean waitingForFeedback = false;
	private static boolean successfulBid = false;
	private static ArrayList<Bid> newBids = new ArrayList<>();
	private static ArrayList<Item> items = new ArrayList<>();
	private String currentItem = null;
	private static boolean quit = false;
	@FXML
	public void placeBid(){
		if(currentItem==null){return;}
		if(timeRemainingLabel.getText().equals("00:00")){return;}
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
			successfulBid=false;
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
				itemDescriptionLabel.setText(i.getDescription());
				if(i.isSold()){itemDescriptionLabel.setText("This Item has been sold to "+i.getOwner()+" for $"+i.getCurrentBid()+"! Please see our other items open for bidding!");}
				Thread timer = new Thread(new Runnable() {
					@Override
					public void run() {
						String myItem = currentItem;
						int timeRemaining = i.getTimeRemaining();
						while(timeRemaining>0){
							try {
								int minutes = timeRemaining/60;
								String seconds;
								if(timeRemaining%60<10){
									seconds = "0"+String.valueOf(timeRemaining%60);
								} else {
									seconds = String.valueOf(timeRemaining%60);
								}
								Platform.runLater(new Runnable(){
									@Override
									public void run(){
										((Client)loader.getController()).setTimer(new String(minutes + ":"+seconds));
									}
								});
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							timeRemaining--;
							if(!myItem.equals(currentItem)){timeRemaining=0;}
						}
						if(myItem.equals(currentItem)) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									itemDescriptionLabel.setText("This Item has been sold to " + i.getOwner() + " for $" + i.getCurrentBid() + "! Please see our other items open for bidding!");
									currentWinner.setText("auction ended");
									currentBid.setText("auction ended");
									timeRemainingLabel.setText("00:00");
								}
							});
						}
					}
				});
				timer.start();
			}
		}
	}
	public void setTimer(String value){
		timeRemainingLabel.setText(value);
	}
	@Override
	public void start(Stage primaryStage) {
		try {
			// Create a socket to connect to the server
			@SuppressWarnings("resource")
			Socket socket = new Socket("192.168.1.125", 5000);//"localhost",5000);//
			// Create an input stream to receive data from the server
			fromServer = new ObjectInputStream(socket.getInputStream());
			// Create an output stream to send data to the server
			toServer = new ObjectOutputStream(socket.getOutputStream());
			Object auction = null;
			while(!((auction=fromServer.readObject()) instanceof Auction)){}
			items = ((Auction)auction).getAuctionItems();
			for(Item i : items){
				i.startTimer();
			}
			// reading and writing threads, to be started upon successful login
			Thread writerThread = new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						while (!quit) {
							if (Client.newBids.size() > 0) {
								System.out.println("sending...");
								toServer.writeObject(Client.newBids.remove(0));
								toServer.flush();
							}
							Thread.sleep(100);
						}
						toServer.writeObject(clientID + " exit");
						toServer.flush();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
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
							if(input instanceof String){
								if (input.equals(clientID + " success")) {
									successfulBid = true;
									setWaitingForFeedback(false);
								} else if (input.equals(clientID + " failed")) {
									successfulBid = false;
									setWaitingForFeedback(false);
								} else if(input.equals(clientID+" stl")){
									System.exit(2);
								}
							}else if(input instanceof Bid) {
								Bid newBid = (Bid) input;
								for (Item i : items) {
									if (newBid.getItemID().equals(i.ID)) {
										i.setCurrentBid(newBid.getBid());
										i.setOwner(newBid.getClientID());
										if(i.getID().equals(currentItem)) {
											Platform.runLater(new Runnable() {
												@Override
												public void run() {
													((Client) loader.getController()).updateScreen(i);
												}
											});
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
			// Create a scene and place it in the stage
			Pane startPane = new Pane();
			startPane.setPrefSize(300, 200);
			Button startButton = new Button("Login");
			Label loginFeedback = new Label("");
			loginFeedback.setLayoutX(77);
			loginFeedback.setLayoutY(10);
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
					toServer.writeObject(new String("login "+username.getText()+" "+password.getText()));
					Object response = null;
					boolean waiting = true;
					while(waiting){
						response = fromServer.readObject();
						System.out.println(response);
						if((response instanceof String)&&((String) response).split(" ")[0].equals("Login")){
							waiting=false;
						}
					}
					if(((String)response).equals("Login success")) {
						clientID = username.getText();
						loader = new FXMLLoader();
						loader.setLocation(getClass().getResource("clientWindow.fxml"));
						primaryStage.setScene(new Scene(loader.load(), 1200, 600)); // Place the scene in the stage
						ObservableList<String> options = FXCollections.observableArrayList();
						for (Item i : items) {
							options.add(i.getID());
						}
						((Client) loader.getController()).setDropdown(options);
						writerThread.start();
						readerThread.start();
					}
					else if(((String)response).equals("Login failed")){
						loginFeedback.setText("Invalid Login. Try again!");
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			});
			startPane.getChildren().add(username);
			startPane.getChildren().add(password);
			startPane.getChildren().add(loginFeedback);
			startPane.getChildren().add(startButton);
			primaryStage.setScene(new Scene(startPane, 300,200));
			primaryStage.setTitle("Client"); // Set the stage title
			primaryStage.show(); // Display the stage
		}
		catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}



	}
	public void logout(){
		quit = true;
	}
	public void setDropdown(ObservableList e){
		currentItemDropdown.getItems().addAll(e);
	}
	private void updateScreen(Item item){
		currentBid.setText(String.valueOf(item.getCurrentBid()));
		currentWinner.setText(item.getOwner());
		if(!item.getOwner().equals(clientID)){feedback.setText("");}
	}
	private void setWaitingForFeedback(boolean value){waitingForFeedback=value;}
	public static void main(String[] args) {
		launch(args);
	}
}
