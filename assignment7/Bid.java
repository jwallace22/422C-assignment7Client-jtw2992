package assignment7;
/**  EE422C Final Project submission by
 * Jeffrey Wallace
 * jtw2992
 * 16310
 * Spring 2020
 */
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
