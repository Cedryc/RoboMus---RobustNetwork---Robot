/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RobustNetwork;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Synch Waiter for RobotEx
 */
public class SynchWaiter extends Thread{
    
    long lastInstrumentTime; // the instrument time at the last synch
    int synchInterval;       // the synch interval communicated by the server
    int missedSynch = 0;     // number of missed synch
    int maxMissedSynch = 4;  // max number of missed synch after which the robots will assume he has lost connection
    
    boolean connectionServer; // true when the robot is connected to the server
    boolean sosAnswered; // true when an sos has been answered
    boolean hasMaster; // true when the robot has a master
    
    boolean serverFailure; // true when the server is failing and robots are on their own
    boolean tempMaster; // true when the robot is the backup master while the server is failing
    /*
    though these two booleans exist, they are unused,
    since my attempts to fully implement the strategy in case of complete server failure didnot work
    
    the idea is the following:
    Server fails => robots will think they have lost connection
    every robots broadcast an sos "lost connection"
    every other robots will answer negatively
    wait a fixed delay of time, after no positive answer, a robot broadcasts "server failure"
    the first robot to broadcast "server failure" becomes a temporary master and he sends sycnh messages based on his own clock
    while "server failure" is true, the first robot to receive the server again broadcasts an "end server failure" message
    the other robots should then receive the server too, or broadcast sos if they don't
    
    the second idea is to have the server send a pre-recorded loop to the robots when he can,
    so that they have messages to play in case the server fails
    those message will be stored in the robots' buffer while the temporary master will do the synch.
    
    It is also possible to choose a temporary master at the beginning of the performance,
    based of the robot's computing power, connection quality,
    or maybe just the drummer-bot as a convention based on real life musicians.
    */
    
    /* 
    if a robot loses connection with the server,
    => broadcasts and SOS 
    => receives no responses at all after the fixed delay,
    ie. no positives ( i can talk with server) but no negatives ( i CAN'T talk with server ) either,
    then the robot should consider itself offline and cease all musical activities until he receives messages again.
    
    */
    
    // coordinates of the robot
    String name;
    String robotAddress;
    String robotOsc;
    int receivePort;
    
    // broadcast IP and coordinates of the sos port
    String ipBroad;
    String SosOsc;
    int SosPort;
    float k = 1;
            
    public SynchWaiter(int synchInterval, String name, String robotAddress, String robotOsc, int receivePort, String ipBroad, String SosOsc){
        
        this.synchInterval = synchInterval;
        
        this.name = name;
        this.robotAddress = robotAddress;
        this.robotOsc = robotOsc;
        this.receivePort = receivePort;
                
        this.ipBroad = ipBroad;
        this.SosOsc = SosOsc;
        this.SosPort = 505;
        
        this.sosAnswered = false;
        this.hasMaster = false;
        
    }
    
    public void SendSos(String type){
                
        OSCPortOut sender = null;
        
        //put the coordinates of the robot in the message, so that whever receives it can answer
        List args = new ArrayList<>();
        args.add(name);
        args.add(robotAddress);
        args.add(robotOsc);
        args.add(receivePort);
        args.add(type);
        
        
        OSCMessage msg = new OSCMessage();
        msg = new OSCMessage(SosOsc, args);
        
        
        
        try {
        sender = new OSCPortOut(InetAddress.getByName( ipBroad ) , 505); //broad cast to all IP addresses i the network at port 505
        } catch (UnknownHostException | SocketException ex) {
        Logger.getLogger(SynchWaiter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
        sender.send(msg);
        } catch (IOException ex) {
        Logger.getLogger(SynchWaiter.class.getName()).log(Level.SEVERE, null, ex);
        }    
            
        /*
        //if the broadcast IP doesn't work, which is possible, use this:
        
        int length = robotAddress.length();
        String ipShort = null;
        int count = 0;
        for (int i=0; i<length-1; i++){
            if (robotAddress.charAt(i) == '.'){
                count++;
            }
            if(count == 3){
                ipShort = robotAddress.substring(0, i+1);
            }
        }
        
        for(int a=1; a<=255; a++){ //broad cast to all 255 IP addresses i the network at port 505
            try {
            sender = new OSCPortOut(InetAddress.getByName( ipShort + Integer.toString(a) ) , 505); 
            } catch (UnknownHostException | SocketException ex) {
            Logger.getLogger(SynchWaiter.class.getName()).log(Level.SEVERE, null, ex);
            }
        
            try {
            sender.send(msg);
            } catch (IOException ex) {
            Logger.getLogger(SynchWaiter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        */
        
        
    
    }
    // the thread that runs in the background
    public void run(){
        /* counts the number of missed synchs in a row
        this makes sure to exit the while loop when the robot loses connection,
        the robot just sends sos messages over and over otherwise
        
        the loop is re-entered when the robot receives synch again through the master
        */
            
        while (missedSynch < maxMissedSynch){ 
            if ((System.currentTimeMillis() - lastInstrumentTime) > (k+0.2)*synchInterval){
                missedSynch ++;
                k ++;
            }
            
            if (missedSynch == maxMissedSynch){ 
                // if the number of missed synch reaches the maximum number, send an sos
                connectionServer = false;
                SendSos("lost connection");
                
            } 
        }
    }
}
    

