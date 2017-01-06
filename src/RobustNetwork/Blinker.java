/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RobustNetwork;

// imports
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.JLabel;
import java.awt.GridLayout;

// for help and explanations: https://docs.oracle.com/javase/tutorial/uiswing/painting/step1.html

public class Blinker extends JPanel{
    // properties of the blinking square
    private int squareX = 50; // position of square
    private int squareY = 50; // position of square
    private int squareW = 400; // width of square
    private int squareH = 300; // height of square
    Color color; // color of the square
    
    public Blinker() {
        
        color = Color.red; //the square starts as red
        setBorder(BorderFactory.createLineBorder(Color.black)); // add a classy and timeless though very discreet lining of black around the square
    }
    
    void paintSquare() { // update the painting (only the color in this case ) of the square
        repaint(squareX,squareY,squareW,squareH);
    }    

    public Dimension getPreferredSize(){  //dimensions of the whole window
        return new Dimension(500,400);
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);       
        g.drawString("This is a musical performance!",10,20); // some text
        g.setColor(color);
        g.fillRect(squareX,squareY,squareW,squareH);
        g.setColor(Color.BLACK);
        g.drawRect(squareX,squareY,squareW,squareH);
    }
       
}

