package RobustNetwork;


import java.net.InetAddress;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * Contact Class for RobotEx
 */
public class Contact {
    
    protected String name;
    protected String contactAddress;
    protected String contactOsc; 
    protected int receivePort;
    
    
    public Contact(String name,String robotAddress, String robotOsc, int sendPort) {
       
        this.name = name;
        this.contactAddress = robotAddress;
        this.contactOsc = robotOsc;
        this.receivePort = sendPort;
    
    }
    
}