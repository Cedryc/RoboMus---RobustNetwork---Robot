/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RobustNetwork;

import com.illposed.osc.OSCMessage;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;


public class BufferEx extends Thread{
    
    private volatile List<OSCMessage> messages; // a list of all the messages
    public long lastServerTime; // the server time at the last synch
    public long lastInstrumentTime; // the instrument time at the last synch
    public int threshold; // the threshold

    public volatile List<Long> sentMessageId;
    Blinker blinker;
    RobotUI robotUI; // create the robot interface
    
    public BufferEx() {
        this.messages = new ArrayList<OSCMessage>();    
        
        this.blinker = new Blinker();
        this.robotUI = new RobotUI();

    }
    
    public void print(){ // print the content of the buffer
        int cont = 0;
        
        System.out.println("_________________buffer______________");
        
        for (OSCMessage message : messages) {
            System.out.println("------------ position = "+cont+" -------------");
            for (Object obj : message.getArguments()) {
               
                System.out.println(obj);
            }
            cont++;
        }
        System.out.println("\n");
    }
    
    public void printLastMessage(){ // print the last message of the buffer
        int ind = messages.size() - 1;
        
        System.out.println("_________________buffer______________");
        System.out.println("------------ position = " + ind + " -------------");
        
        for (Object obj : messages.get(ind).getArguments()) {
                System.out.println(obj);
        }
        System.out.println("\n");
    }
    
    public OSCMessage remove(){ // remove the first message received
        return messages.remove(0);
    }
    
    public void add(OSCMessage message){ // add 1 message to the buffer
        messages.add(message);    
    }
    
    public void remove(int n){ // remove a specific message
        for (int i = 0; i < n; i++) {
            messages.remove(i);
        }
    }
       
    // relativeTime = server Time at last synch + time elapsed since. time elapsed gets reset to 0 at every synch.
    // time elapsed is calculated with currentTime - lastInstrumentTime.
    // this is why the synch is used, to make sure that every robot's relativeTime value is equal to the server's currentTime value
    public long relativeTime(){
        return (this.lastServerTime + ( System.currentTimeMillis() - this.lastInstrumentTime) ); 
    }        
    
    // sets up and create and show the Blinker
    static void createAndShowGUI(Blinker blinker){
        
        JFrame frame = new JFrame("I am a robot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());
        frame.add(blinker);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    public void run() {
        long timePlay;
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                createAndShowGUI(blinker); // display the blinker
                robotUI.setVisible(true);  // display the User Interface
            }
        }); 
        
        while(true){
            
            // wait for messages
            if (!this.messages.isEmpty()) {
                try {
                Thread.sleep(10); //wait 10ms before processing the message, to make sure everything is there
                } 
                catch (InterruptedException ex) {
                Logger.getLogger(BufferEx.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                OSCMessage message = messages.get(0);
                List L = message.getArguments(); 
                // arguments of the message, for now contains only colors
                // to make it functionnal, the arguments should contain instrument related instructions such as notes, frets, etc
                
                // when to send the instruction to the hardware, or the blinker
                timePlay = (long)L.get(3);
                
                // first test: is it time to play the note ?
                // second test: is it too late to play the note ?
                // if  (relativeTime() - timePlay) <= this.threshold, it is too late to play
                if ( relativeTime() >= timePlay   &&   (relativeTime() - timePlay) <= this.threshold ) {
                    
                    try { // convert the String "color" into a Color object that can be interpreted by the painter
                    Field field = Class.forName("java.awt.Color").getField((String)L.get(4));
                    robotUI.print("i received " + (String)L.get(4) + "\n");
                    blinker.color = (Color)field.get(null);
                    } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    blinker.color = null; // Not defined
                    }
                
                    blinker.paintSquare(); // paint the square
                
                    remove();                
                }      
            }
        }
    }
    
    
}
