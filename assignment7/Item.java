package assignment7;

import java.io.Serializable;
import java.util.Observable;

public class Item extends Observable implements Serializable {
    protected String ID;
    private double currentBid = 0;
    private int timeLimit;
    protected int timeRemaining;
    private boolean sold = false;
    private String myDesciption;
    private String owner = "no bid placed";
    public Item(String id,double min,int time,String description){
        ID = id;
        myDesciption=description;
        timeLimit=time;
        currentBid=min;
        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                int timeRemaining=time;
                while(timeRemaining>0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    setTimeRemaining(timeRemaining--);
                }
                sold = true;
            }
        });
        timer.start();
    }
    private void setTimeRemaining(int time){timeRemaining=time;}
    public String getDescription(){return myDesciption;}
    public String getID(){return ID;}
    public double getCurrentBid(){
        return currentBid;
    }
    public String getOwner(){
        return owner;
    }
    public int getTimeRemaining() {
        return timeRemaining;
    }
    public boolean isSold() {
        return sold;
    }
    public void setCurrentBid(Double newBid){currentBid=newBid;}
    public void setOwner(String winner){owner=winner;}
    public boolean placeBid(Bid newBid){
        if(sold){return false;}
        if(newBid.getBid() <= currentBid){return false;}
        else{
            currentBid = newBid.getBid();
            owner = newBid.getClientID();
            return true;
        }
    }
}
