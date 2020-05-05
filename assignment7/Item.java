package assignment7;

import java.io.Serializable;
import java.util.Observable;

public class Item extends Observable implements Serializable {
    protected String ID;
    private double currentBid = 0;
    private int timeLimit;
    protected int timeRemaining;
    private boolean sold = false;
    private String owner = "no bid placed";
    public Item(String id,double min,int time){
        ID = id;
        timeLimit=time;
        currentBid=min;
        timeRemaining=time;
        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                while(timeRemaining>0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timeRemaining--;
                    //System.out.println(timeRemaining);//debugging statement for time
                }
                sold = true;
            }
        });
        timer.start();
    }
    public String getID(){return ID;}
    public double getCurrentBid(){
        return currentBid;
    }
    public String getOwner(){
        return owner;
    }
    public int getTimeLimit() {
        return timeLimit;
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
