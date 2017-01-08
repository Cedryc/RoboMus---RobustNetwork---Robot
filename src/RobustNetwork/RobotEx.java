/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/*
HOW TO COMPILE INTO A RUNNABLE JAR FILE:
Right-click on the project name;
=> Properties
   => packaging
      => tick every boxes
Right-click on the project name;
=> clean and build

go to the project folder
=> go to the dist folder
  => [name of project].jar can be run in any computer with a java machine (most computers have one )
*/



/*
ADDITIONNAL INFOS 
on the two un-implemented strategies in case of errors: "server failure" and "robot loses all connections"
can be found in class SynchWaiter where the two strategy should have been implemented
*/






package RobustNetwork;

// imports
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;
import java.util.Timer;


public class RobotEx {
    
    // variables declaration
    protected BufferEx buffer; // the buffer of the robot
    protected SynchWaiter synchWaiter; // the synch waiter of the robot
    
    protected String name; // name of the robot, to be used when communicating
    protected String robotAddress; // Ip address of the robot, to be sent to other robots/server during handshake to set up communications
    protected String robotOsc; //OSC address of the robot
    protected String serverAddress; // Ip address of the server
    protected String serverOsc; // OSC address of the server
    protected int sendPort; //the port of the robot used to send messages
    protected int receivePort; // the port used by the server to receive messages
    // the Ip address and OSC address and receive port of the server is not a variable in ServerEx because the server Ip is inputed in the robots by the user through the UI
    protected int synchInterval; // the interval during each synch messages, communicated by the server
    
    protected Timer timer; // a timer
    
    // IpAddress of the robot
    String ipAddr; 
    // broadcast Ip, https://en.wikipedia.org/wiki/Broadcast_address
    String ipBroad;
    
    // the coordinates of the Sos port every robots listen to
    String SosOsc;
    int SosPort;
    
    static List<Contact> contacts; // the contacts list of the robots
    /*
    in normal conditions, the contacts list is empty
    the robot adds in his contact list every other robots of who he becomes the master
    he then removes them when the problem ends
    */
    
    static Contact server; // creat a server contact
    static Contact master; // creat a master contect
    
    
    public RobotEx(int receivePort) {
        
        // assign fake values to robot name/Osc and Server coordinates
        // the real values used will be input by the user every time the robot is launched
        this.name = "NaN";
        this.robotOsc = "NaN";
        this.serverAddress = "NaN";
        this.serverOsc = "NaN";
        this.sendPort = 0;
        this.receivePort = receivePort; // assign the value of the receiving port
        
        Timer timer = new Timer();
        
        //start the buffer
        this.buffer = new BufferEx();
        this.buffer.start();
                
        try {  //this gives the ip address of the robot
            this.ipAddr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.robotAddress = ipAddr;
        
        // this calculate the broadcast address of the networt
        // it takes your regular IP and replaces the last number with 255, ex: "192.168.1.120" => "192.168.1.255"
        // this definiton of broadcast IP may be flawed
        int length = ipAddr.length();
        String ipBroad = null;
        int count = 0;
        for (int i=0; i<length-1; i++){
            if (ipAddr.charAt(i) == '.'){
                count++;
            }
            if(count == 3){
                String ipShort = ipAddr.substring(0, i+1);
                    ipBroad = ipShort + "255";
                    break;
            }
        }
        
        this.contacts = new ArrayList<Contact>();
        this.server = new Contact("NaN",this.serverAddress,this.serverOsc,this.sendPort);
        this.SosOsc = "/SOS";
        this.SosPort = 505; // 505 is arbitrary, but it looks like "SOS" so easier to rememember
        
        this.master = null;
    }
    
    /* Message structure:
    Contact of Origin, Contact destination, type of message, other args.
    */
    
    // sender function, same template than ServerSender in ServerEx
    public void RobotSender(Contact target, String type, String message){
                
        OSCPortOut sender = null;
        
        try {
        sender = new OSCPortOut(InetAddress.getByName(target.contactAddress) , target.receivePort);
        } catch (UnknownHostException | SocketException ex) {
        Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // every message contains: name of sender, name of target, type of message
        List args = new ArrayList<>();
        args.add(this.name); 
        args.add(target.name);
        args.add(type);
        
        switch(type){
            
            /* an idea could be to replace the full words messages tpyes with single numbers,
            to reduce the size of messages
            */
            
            case "handshake": // if the message is a handshake, the robot transmits his own coordinates to the target
                args.add(this.robotAddress);
                args.add(this.robotOsc);
                args.add(this.receivePort);
                break;
                            
            case "test back": // send a simple "test back" message to confirm to the server that the connection works
                break;
            
            // is used to answer to an Sos message
            // the robot transmitst his coordinates to perform a handshake with the robot who sent Sos
            case "i can talk with server":
                args.add(this.robotAddress);
                args.add(this.robotOsc);
                args.add(this.receivePort);
                break;
            
            // is used to answer to an Sos message
            case "i can NOT talk with server":
                break;
            
            // is used to answer to an " i can talk with server" message
            // the robot sends his coordinates to perform a handshake so that the master can save them
            case "you are master":
                args.add(this.robotAddress);
                args.add(this.robotOsc);
                args.add(this.receivePort);
                break;
                
            // is used to signal the master than communications got back to normal
            case "you are not master anymore":
                break;
                
            // is used to answer to a "you are master message", sends a message to the server including the name of the deficient robot
            // server will interpret like this: the name in the message has a problem, the sender of the message is his master
            case "help for R":
                args.add(message); // message is the name of the deficient robot
                break;
            
            // used to disconnect the robot from the performance
            // there is no way in the main function/ User Interface to actually send this message
            case "sign out": 
                break;
        }
        
        OSCMessage msg = new OSCMessage(target.contactOsc, args);

        try {
        sender.send(msg);
        buffer.robotUI.print("message " + type + " sent to " + target.name + "\n");
        } catch (IOException ex) {
        Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }
    
    // thread to receive messages directed towards this robot
    public void RobotReiceiver(){
        
        OSCPortIn receiver = null;
        
        try {
            receiver = new OSCPortIn(this.receivePort);
        } 
        catch (SocketException ex) {
            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        OSCListener listener = new OSCListener() {
            
            public void acceptMessage(java.util.Date time, OSCMessage message) {
                
                //create a list with the contents of the message
                List L = message.getArguments();
                
                // test to determine if the name of the target specified in the message is different than this robot's name
                // this will happen only if a robot has a connection problem
                // the master will then receive messages for the other robot and use this test to differenciate his own message from the ones he need to redirect
                if (!((String)L.get(1)).equals(name)){ // 
                    for(Contact contact : contacts){
                        // find whose name it is in the contact list
                        // the list will contain robots only if this robot is the master of someone else
                        if (contact.name.equals((String)L.get(1))){
                            
                            // a sender function
                            // simply resends the contents of the message to the actual target
                            OSCPortOut sender = null;
        
                            try {
                            sender = new OSCPortOut(InetAddress.getByName(contact.contactAddress) , contact.receivePort);
                            } catch (UnknownHostException | SocketException ex) {
                            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            OSCMessage msg = new OSCMessage(contact.contactOsc, L);
                                                        
                            try {
                            sender.send(msg);
                            buffer.robotUI.print("message " + (String)L.get(2) + " from " + (String)L.get(0) + " sent to " + contact.name + "\n");
                            } catch (IOException ex) {
                            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                        }
                    }
                }
                
                else{
                    
                    
                    // filter between the different types of messages
                    switch((String)L.get(2)){
                            
                        case "message to R":
                            buffer.add(message);
                            break;
                        
                        case "i can talk with server": 
// this robot has already sent an Sos and now other robots are responding
                            
                            synchWaiter.sosAnswered = true;
                            // the Sos has been answered, can be used to determine if a robot has lost all connections or only the server
                            // no backup plan is included in the program in case the robot has lost all connection
                            
                            /*
                            this test implements the election process:
                            the first robot to answer will become the master and will receive a "you are master" message
                            */
                            if (synchWaiter.hasMaster == false){
                                synchWaiter.hasMaster = true;
                                master = new Contact((String)L.get(0), (String)L.get(3), (String)L.get(4), (int)L.get(5));
                                RobotSender(master, "you are master", null);
                            }
                            break;
                           
                        case "i can NOT talk with server":
                            synchWaiter.sosAnswered = true;
                            // the Sos has been answered, can be used to determine if a robot has lost all connections or only the server
                            // no backup plan is included in the program in case the robot has lost all connection
                            break;
                            
                        case "you are master": 
                            // in case this robot responded first to an Sos message
                            // he will receive a message signaling he is the master
                            // he will then add the robot who sent the Sos to his contacts
                            Contact contact = new Contact((String)L.get(0), (String)L.get(3), (String)L.get(4), (int)L.get(5));
                            contacts.add(contact);
                            // as the master, this robot will tell the server to send messages here instead
                            RobotSender(server, "help for R", contact.name);
                            break;
                        
                        case "you are not master anymore":
                            // when communications go back to normal for the other robots, he will tell this one that he is not master anymore
                            // the former master will then remove his former slave from his contacts
                            for(Contact contactSlave : contacts){
                                if (contactSlave.name.equals((String)L.get(0))){
                                    contacts.remove(contactSlave);                            
                                }
                            }
                            break;
                            
                        case "test answer":
                            // when a robot loses connection with server, the server will send "test answer" messages to the robot
                            // once the robot receives one of those messages it answers with "test back"
                            RobotSender(server, "test back", null); // answers the server
                            synchWaiter.connectionServer = true;
                            master = null;
                            synchWaiter.hasMaster = false;
                            synchWaiter.tempMaster = false;
                            contacts.clear();
                            break;
                            
                        case "synch start":
                            // the first synch message adds additionnal info
                            buffer.lastServerTime = (Long)L.get(3);
                            buffer.lastInstrumentTime = System.currentTimeMillis();
                            synchInterval = (int)L.get(4); // the synch interval, so that the robot can anticipate when synch comes
                            buffer.threshold = (int)L.get(5); 
                            buffer.robotUI.print("synch start\n");
                            buffer.robotUI.print("This my threshold: " + buffer.threshold + "\n");
                            buffer.robotUI.print("This is the synch interval: " + synchInterval + "\n");
                            
                            // restarts the synch waiter
                            synchWaiter = new SynchWaiter(synchInterval, name, robotAddress, robotOsc, receivePort, ipBroad, SosOsc);
                            synchWaiter.lastInstrumentTime = buffer.lastInstrumentTime;
                            synchWaiter.start();
                            break;
                        
                        case "synch": // every synch messages transmit the current server time
                            buffer.lastServerTime = (Long)L.get(3); // update the last server time 
                            buffer.lastInstrumentTime = System.currentTimeMillis(); // update the last instrument time
                            synchWaiter.lastInstrumentTime = buffer.lastInstrumentTime;
                            synchWaiter.missedSynch = 0; // resets the missed synch counter
                            synchWaiter.k = 1; // resets the value of k
                            break;
                    }   
                }
            }
        };
        
        receiver.addListener(this.robotOsc, listener);
        receiver.startListening(); 
        buffer.robotUI.print("Listening to server\n");       
    }
    
    // Thread to wait for SOS broadcasts and answer accordingly    
    // this receiver listens specifically to the sos coordinates
    // these sos coordinates are the same for all robots
    public void SosReiceiver(){
        
        OSCPortIn receiver = null;
        
        try {
        receiver = new OSCPortIn(SosPort);
        } 
        catch (SocketException ex) {
        Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        OSCListener listener = new OSCListener() {
            
            // protocols to follow when a sos message arrives
            
            public void acceptMessage(java.util.Date time, OSCMessage message) {
                
                // create a list with the contents of the message
                List L = message.getArguments();
                if (!((String)L.get(0)).equals(name)){
                    Contact contact = new Contact((String)L.get(0), (String)L.get(1), (String)L.get(2), (int)L.get(3));
                    
                    // different actions depending on the type ( inside L.get(4) ) of message
                    switch ((String)L.get(4)){
                    
                        // if the SOS broadcast is simply a robot who lost connection
                        case "lost connection":
                    
                            buffer.robotUI.print((String)L.get(0) + " lost server and asks for help\n");
                            
                            // if this robot has connection with the server, he will send a positive answer to the sos sender
                            if (synchWaiter.connectionServer == true){
                                RobotSender(contact, "i can talk with server", null);
                            }
                            // if this robot does not have connexion to the server, he will send a negative answer to the sos sender
                            // sending a negative answer is a way for the sos sender that he can still talk with other robots
                            else{
                                RobotSender(contact, "i can NOT talk with server", null);
                            }
                            break;
                        
                        // if the SOS broadcast is a robot warning of a server failure
                        // the robot that broadcasts the warning becomes everyone's master
                        case "server failure":
                            
                            synchWaiter.serverFailure = true;
                            master = contact;
                            RobotSender(master, "you are master", null);
                            break;
                    }
                }
            }   
                    
        };
        
        receiver.addListener(SosOsc, listener);
        receiver.startListening(); 
        buffer.robotUI.print("Listening to sos\n");
    }    
    
    
    public static void main(String[] args) {
        Scanner intscan = new Scanner(System.in);
        
        RobotEx Robot = new RobotEx( 111); // create a robot
        // 111 is arbitrary, possible to add it as an input in the UI      
        
        /*
        the next While loops are used to wait for user inputs
        though there must be a more efficient way to do this
        */
        Robot.buffer.robotUI.ask("Enter a name for the Robot");
        while (Robot.name.equals("NaN")){
            Robot.name = Robot.buffer.robotUI.inputText;
            // once a user inputs anything different tan "NaN", the name is saved and the while loop exits
            System.out.println(Robot.name);
            try{
            Thread.sleep(200);
            } catch (InterruptedException ex) {
            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Robot.buffer.robotUI.inputText = "NaN"; // resets the input value to "NaN" so that the next loop can function
        Robot.buffer.robotUI.print("My name is " + Robot.name + "\n");
        Robot.robotOsc = "/" + Robot.name; // creates the Osc address from the name: Osc address = /name
        Robot.buffer.robotUI.print("My OSC address is " + Robot.robotOsc + "\n");

        /* 
        the server coordinates are displayed by the Netbeans console when the server is launched
        another user interface for the server would be more usable
        */
        
        
        // same loop for inputing Server name
        Robot.buffer.robotUI.ask("Enter a name for the Server");
        while (Robot.server.name.equals("NaN")){
            Robot.server.name = Robot.buffer.robotUI.inputText;
            System.out.println(Robot.server.name);
            try{
            Thread.sleep(200);
            } catch (InterruptedException ex) {
            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Robot.buffer.robotUI.inputText = "NaN";
        Robot.buffer.robotUI.print("The Server name is " + Robot.server.name + "\n");
        Robot.server.contactOsc = "/" + Robot.server.name;
        Robot.buffer.robotUI.print("The Server OSC address is " + Robot.server.contactOsc + "\n");
        
        // loop for inputing server ip address
        Robot.buffer.robotUI.ask("Enter an Ip address for the Server");
        while (Robot.server.contactAddress.equals("NaN")){
            Robot.server.contactAddress = Robot.buffer.robotUI.inputText;
            System.out.println(Robot.server.contactAddress);
            try{
            Thread.sleep(200);
            } catch (InterruptedException ex) {
            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Robot.buffer.robotUI.inputText = "0";
        Robot.buffer.robotUI.print("The Server Ip is " + Robot.server.contactAddress + "\n");        
        
        // loop for inputing server receiver port
        Robot.buffer.robotUI.ask("Enter a receive port for the Server");
        while (Robot.server.receivePort == 0){
            Robot.server.receivePort = Integer.parseInt(Robot.buffer.robotUI.inputText);
            System.out.println(Robot.server.receivePort);
            try{
            Thread.sleep(200);
            } catch (InterruptedException ex) {
            Logger.getLogger(RobotEx.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Robot.buffer.robotUI.print("The Server port is " + Robot.server.receivePort + "\n");
        
        
        Robot.buffer.robotUI.print("My Ip adress is " + Robot.ipAddr + "\n");
        Robot.buffer.robotUI.print("i broadcast to " + Robot.ipBroad + "\n");
        
        Robot.buffer.robotUI.print("\n" + Robot.server.name + " " + Robot.server.contactOsc + " " + Robot.server.contactAddress + " " + Robot.server.receivePort + "\n");
        
        // start the listeners for messages and SOS broadcasts
        Robot.RobotReiceiver();
        Robot.SosReiceiver();
        // send handshake to server automatically when robot starts
        // need to start server before robots
        Robot.RobotSender(server, "handshake", null);

        
        // tests to print the current state of the robot
        while(true){
            int i = intscan.nextInt();
            if (i == 1){
                Robot.buffer.print();
            }    
            if(i == 4){
                Robot.synchWaiter.SendSos("lost connexion");
            }
            if (i == 5){
                System.out.println(contacts);
            }
            if(i == 6){
                System.out.println(Robot.synchInterval);
                System.out.println(System.currentTimeMillis() - Robot.buffer.lastInstrumentTime);
                System.out.println(Robot.synchWaiter.missedSynch);
            }
            if(i == 7){
                System.out.println("Robot State is:");
                System.out.println("robotHasMaster = " + Robot.synchWaiter.hasMaster);
                System.out.println("connexionServer = " + Robot.synchWaiter.connectionServer);
                System.out.println("sosAnswered = " + Robot.synchWaiter.sosAnswered);
                System.out.println("serverFailure = " + Robot.synchWaiter.serverFailure);
                System.out.println("tempMaster = " + Robot.synchWaiter.tempMaster);
            }
        }         
    }
}