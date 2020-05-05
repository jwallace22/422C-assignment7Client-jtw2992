package assignment7;

import java.io.Serializable;

public class Bid implements Serializable {
    private static long serialVersionUID = 1L;
    private String clientID;
    private String itemID;
    private Double myBid;
    public Bid(String client,Double bid, String item){
        clientID=client;
        myBid=bid;
        itemID=item;
    }
    public Double getBid() {
        return myBid;
    }
    public String getClientID() {
        return clientID;
    }
    public String getItemID() {
        return itemID;
    }
    @Override
    public String toString(){
        return clientID +" "+ myBid.toString() +" "+ itemID;
    }
}
