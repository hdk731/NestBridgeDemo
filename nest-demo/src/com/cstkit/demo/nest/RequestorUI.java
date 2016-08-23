package com.cstkit.demo.nest;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;



@SuppressWarnings("serial")
public class RequestorUI extends JFrame 
{
    public static final int HEIGHT = 800;
    public static final int WIDTH = 310;

    JPanel pnlRequestor;

    // Set visual elements for Requestor 
    JPanel pnlSet;
    JButton btnSetTarget;
    JButton btnSetHvac;
    JSpinner spinnerSetTarget;
    JComboBox<String> hvacComboBox;
    ButtonGroup btnGroupSet;
    JLabel lblSetResults;
    JButton btnSetTargetHigh;
    JButton btnSetTargetLow;
    JSpinner spinnerSetTargetHigh;
    JSpinner spinnerSetTargetLow;

    // Get visual elements for Requestor 
    JPanel pnlGet;
    JButton btnGetOp;
    JLabel lblGetAmbient;
    JLabel lblGetTarget;
    JLabel lblGetHvac;

    // Subscribe visual elements for Requestor 
    JPanel pnlSubscribe;
    JButton btnSubscribeAmbientOp;
    JLabel lblSubscribeAmbient;
    JButton btnSubscribeAllOp;
    JLabel lblSubscribeTarget;
    JLabel lblSubscribeTargetHigh;

    ActionListener actionListener = new NestUIActionListener();

    Requestor requestor;
    public RequestorUI requestorPar = this;
    String providerOID;

    private JLabel lblGetTargetHigh;
    private JLabel lblGetTargetLow;
    private JLabel lblGetHumidity;
    private JLabel lblSubscribeTargetLow;
    private JLabel lblSubscribeHumidity;
    private JLabel lblSubscribeHvac;
    private JLabel lblGetName;
    private JLabel lblGetTempScale;
    private JLabel lblGetAmbientResults;
    private JLabel lblGetHumidityResults;
    private JLabel lblGetHvacResults;
    private JLabel lblGetTargetResults;
    private JLabel lblGetTargetHighResults;
    private JLabel lblGetTargetLowResults;
    private JLabel lblGetNameResults;
    private JLabel lblGetTempScaleResults;
    private JLabel lblSubscribeAmbientResults;
    private JLabel lblSubscribeHumidityResults;
    private JLabel lblSubscribeTargetResults;
    private JLabel lblSubscribeTargetHighResults;
    private JLabel lblSubscribeTargetLowResults;
    JLabel lblSubscribeAwayHighResults;
    JLabel lblSubscribeAwayLowResults;
    JLabel lblSubscribeHasLeafResults;
    JLabel lblSubscribeCanHeatResults;
    JLabel lblSubscribeCanCoolResults;
    JLabel lblGetVersionResults;
    private JLabel lblSubscribeHvacResults;
    private JLabel lblDeviceID;
    private JLabel lblGetDeviceIDResults;

    public RequestorUI(String providerOID) throws IOException{
        setResizable(false);
        setIconImage(Toolkit.getDefaultToolkit().getImage("C:\\projects\\NEST_protocol_bridge\\logos\\logo.png"));
        this.providerOID = providerOID;
        initUI();
    }
    
    // Initialize user interface
    private void initUI() throws IOException{
        this.addWindowListener(new WindowCloseHandler());
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setTitle("NEST Requestor");
        
        
        // Declare fonts
        Font defaultFont = new Font("Microsoft Sans Serif", Font.PLAIN, 14);
        Font boldFont = new Font("Consolas", Font.BOLD, 18);

        // Requestor1 panel setup
        pnlRequestor = new JPanel();
        pnlRequestor.setBounds(10, 10, 517, 750);
        pnlRequestor.setLayout(null);

        // DOF, CST and NEST icons
        ImageIcon dofImage = new ImageIcon("logos/opendof.png");
        JLabel lblDofImg = new JLabel(dofImage, SwingConstants.CENTER);
        lblDofImg.setText("");
        lblDofImg.setLocation(133, 5);
        lblDofImg.setSize(227, 57);
        ImageIcon nestImage = new ImageIcon("logos/nest.png");
        JLabel lblNestImg = new JLabel(nestImage, SwingConstants.CENTER);
        lblNestImg.setText("");
        lblNestImg.setLocation(370, 5);
        lblNestImg.setSize(137, 57);
        ImageIcon cstImage = new ImageIcon("logos/cst-logo.png");
        JLabel lblCSTImg = new JLabel(cstImage, SwingConstants.CENTER);
        lblCSTImg.setText("");
        lblCSTImg.setLocation(5, 5);
        lblCSTImg.setSize(127, 57);
        pnlRequestor.add(lblDofImg);
        pnlRequestor.add(lblNestImg);
        pnlRequestor.add(lblCSTImg);
        
        // Get panel setup
        pnlGet = new JPanel();
        pnlGet.setBounds(10, 59, 497, 219);
        pnlGet.setBorder(BorderFactory.createTitledBorder("Get operations"));
        pnlGet.setLayout(null);

        // Get visual elements
        btnGetOp = new JButton("GET ALL");
        btnGetOp.addActionListener(actionListener);
        btnGetOp.setFont(defaultFont);
        btnGetOp.setMargin(new Insets(0, 0, 0, 0));
        btnGetOp.setIconTextGap(0);
        btnGetOp.setVerticalTextPosition(AbstractButton.CENTER);
        btnGetOp.setHorizontalTextPosition(AbstractButton.CENTER);
        btnGetOp.setBounds(10, 22, 125, 29);

        lblGetAmbient = new JLabel("Ambient Temp: ");
        lblGetAmbient.setFont(defaultFont);
        lblGetAmbient.setBounds(10, 62, 106, 20);

        lblGetTarget = new JLabel("Target Temp: ");
        lblGetTarget.setFont(defaultFont);
        lblGetTarget.setBounds(10, 124, 106, 20);

        lblGetHvac = new JLabel("HVAC Mode: ");
        lblGetHvac.setFont(defaultFont);
        lblGetHvac.setBounds(252, 186, 97, 20);

        // Add get elements to get panel
        pnlGet.add(btnGetOp);
        pnlGet.add(lblGetAmbient);
        pnlGet.add(lblGetTarget);
        pnlGet.add(lblGetHvac);

        // Set panel setup
        pnlSet = new JPanel();
        pnlSet.setBounds(10, 289, 497, 190);
        pnlSet.setBorder(BorderFactory.createTitledBorder("Set operations"));
        pnlSet.setLayout(null);

        spinnerSetTarget = new JSpinner();
        spinnerSetTarget.setFont(defaultFont);
        spinnerSetTarget.setModel(new SpinnerNumberModel(new Integer(72), null, null, new Integer(1)));
        spinnerSetTarget.setBounds(140, 24, 51, 28);

        hvacComboBox = new JComboBox<>();
        hvacComboBox.setFont(defaultFont);
        hvacComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"heat", "cool", "heat-cool", "off"}));
        hvacComboBox.setBounds(393, 27, 73, 20);

        btnSetTarget = new JButton("Target Temp");
        btnSetTarget.setEnabled(true);
        btnSetTarget.setMargin(new Insets(0, 0, 0, 0));
        btnSetTarget.addActionListener(actionListener);
        btnSetTarget.setFont(defaultFont);
        btnSetTarget.setBounds(10, 23, 100, 28);

        btnSetHvac = new JButton("HVAC mode");
        btnSetHvac.setEnabled(true);
        btnSetHvac.setMargin(new Insets(0, 0, 0, 0));
        btnSetHvac.addActionListener(actionListener);
        btnSetHvac.setFont(defaultFont);
        btnSetHvac.setBounds(258, 23, 100, 28);

        lblSetResults = new JLabel();
        lblSetResults.setHorizontalAlignment(SwingConstants.CENTER);
        lblSetResults.setText("Last Set operation:  Undefined");
        lblSetResults.setFont(defaultFont);
        lblSetResults.setBounds(10, 146, 477, 44);

        // Add set elements to set panel
        pnlSet.add(btnSetTarget);
        pnlSet.add(btnSetHvac);
        pnlSet.add(lblSetResults);
        pnlSet.add(hvacComboBox);
        pnlSet.add(spinnerSetTarget);

        // Subscribe panel setup
        pnlSubscribe = new JPanel();
        pnlSubscribe.setBounds(10, 490, 497, 249);
        pnlSubscribe.setBorder(BorderFactory.createTitledBorder("Subscribe Operations"));
        pnlSubscribe.setLayout(null);

        // Subscribe visual elements
        btnSubscribeAmbientOp = new JButton("Ambient Temperature");
        btnSubscribeAmbientOp.setEnabled(true);
        btnSubscribeAmbientOp.addActionListener(actionListener);
        btnSubscribeAmbientOp.setFont(defaultFont);
        btnSubscribeAmbientOp.setBounds(72, 19, 187, 30);

        btnSubscribeAllOp = new JButton("ALL");
        btnSubscribeAllOp.setEnabled(true);
        btnSubscribeAllOp.addActionListener(actionListener);
        btnSubscribeAllOp.setFont(defaultFont);
        btnSubscribeAllOp.setBounds(296, 19, 94, 30);

        lblSubscribeAmbient = new JLabel("Ambient Temp:");
        lblSubscribeAmbient.setBounds(10, 60, 105, 20);
        lblSubscribeAmbient.setFont(defaultFont);

        lblSubscribeTarget = new JLabel("Target Temp:");
        lblSubscribeTarget.setBounds(10, 122, 105, 20);
        lblSubscribeTarget.setFont(defaultFont);

        // Add subscribe elements to subscribe panel
        pnlSubscribe.add(btnSubscribeAmbientOp);
        pnlSubscribe.add(btnSubscribeAllOp);
        pnlSubscribe.add(lblSubscribeAmbient);
        pnlSubscribe.add(lblSubscribeTarget);

        // Add panels to requestor1 panel
        pnlRequestor.add(pnlSet);
        pnlRequestor.add(pnlGet);

        lblGetTargetHigh = new JLabel("Target High: ");
        lblGetTargetHigh.setFont(defaultFont);
        lblGetTargetHigh.setBounds(10, 155, 106, 20);
        pnlGet.add(lblGetTargetHigh);

        lblGetTargetLow = new JLabel("Target Low: ");
        lblGetTargetLow.setFont(defaultFont);
        lblGetTargetLow.setBounds(10, 186, 106, 20);
        pnlGet.add(lblGetTargetLow);

        lblGetHumidity = new JLabel("Humidity:");
        lblGetHumidity.setFont(defaultFont);
        lblGetHumidity.setBounds(10, 93, 106, 20);
        pnlGet.add(lblGetHumidity);

        lblGetName = new JLabel("Name:");
        lblGetName.setFont(defaultFont);
        lblGetName.setBounds(252, 124, 58, 20);
        pnlGet.add(lblGetName);

        lblGetTempScale = new JLabel("Temperature Scale: ");
        lblGetTempScale.setFont(defaultFont);
        lblGetTempScale.setBounds(252, 155, 138, 20);
        pnlGet.add(lblGetTempScale);

        lblGetAmbientResults = new JLabel("");
        lblGetAmbientResults.setFont(boldFont);
        lblGetAmbientResults.setBounds(150, 62, 47, 20);
        pnlGet.add(lblGetAmbientResults);

        lblGetHumidityResults = new JLabel("");
        lblGetHumidityResults.setFont(boldFont);
        lblGetHumidityResults.setBounds(150, 93, 47, 20);
        pnlGet.add(lblGetHumidityResults);

        lblGetHvacResults = new JLabel("");
        lblGetHvacResults.setHorizontalAlignment(SwingConstants.RIGHT);
        lblGetHvacResults.setFont(boldFont);
        lblGetHvacResults.setBounds(395, 186, 92, 20);
        pnlGet.add(lblGetHvacResults);

        lblGetTargetResults = new JLabel("");
        lblGetTargetResults.setFont(boldFont);
        lblGetTargetResults.setBounds(150, 124, 47, 20);
        pnlGet.add(lblGetTargetResults);

        lblGetTargetHighResults = new JLabel("");
        lblGetTargetHighResults.setFont(boldFont);
        lblGetTargetHighResults.setBounds(150, 155, 47, 20);
        pnlGet.add(lblGetTargetHighResults);

        lblGetTargetLowResults = new JLabel("");
        lblGetTargetLowResults.setFont(boldFont);
        lblGetTargetLowResults.setBounds(150, 186, 47, 20);
        pnlGet.add(lblGetTargetLowResults);

        lblGetNameResults = new JLabel("");
        lblGetNameResults.setHorizontalAlignment(SwingConstants.RIGHT);
        lblGetNameResults.setFont(boldFont);
        lblGetNameResults.setBounds(349, 124, 138, 20);
        pnlGet.add(lblGetNameResults);

        lblGetTempScaleResults = new JLabel("");
        lblGetTempScaleResults.setHorizontalAlignment(SwingConstants.RIGHT);
        lblGetTempScaleResults.setFont(boldFont);
        lblGetTempScaleResults.setBounds(440, 155, 47, 20);
        pnlGet.add(lblGetTempScaleResults);

        lblDeviceID = new JLabel("Device ID:");
        lblDeviceID.setFont(defaultFont);
        lblDeviceID.setBounds(252, 26, 82, 20);
        pnlGet.add(lblDeviceID);

        lblGetDeviceIDResults = new JLabel("");
        lblGetDeviceIDResults.setHorizontalAlignment(SwingConstants.RIGHT);
        lblGetDeviceIDResults.setFont(defaultFont);
        lblGetDeviceIDResults.setBounds(193, 49, 294, 20);
        pnlGet.add(lblGetDeviceIDResults);

        JLabel lblSoftwareVersion = new JLabel("Software Version:");
        lblSoftwareVersion.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 14));
        lblSoftwareVersion.setBounds(252, 93, 117, 20);
        pnlGet.add(lblSoftwareVersion);

        lblGetVersionResults = new JLabel("");
        lblGetVersionResults.setHorizontalAlignment(SwingConstants.RIGHT);
        lblGetVersionResults.setFont(new Font("Consolas", Font.BOLD, 18));
        lblGetVersionResults.setBounds(370, 93, 117, 20);
        pnlGet.add(lblGetVersionResults);
        pnlRequestor.add(pnlSubscribe);

        lblSubscribeTargetHigh = new JLabel("Target High:");
        lblSubscribeTargetHigh.setFont(defaultFont);
        lblSubscribeTargetHigh.setBounds(10, 153, 94, 20);
        pnlSubscribe.add(lblSubscribeTargetHigh);

        lblSubscribeTargetLow = new JLabel("Target Low:");
        lblSubscribeTargetLow.setFont(defaultFont);
        lblSubscribeTargetLow.setBounds(10, 184, 105, 20);
        pnlSubscribe.add(lblSubscribeTargetLow);

        lblSubscribeHumidity = new JLabel("Humidity:");
        lblSubscribeHumidity.setFont(defaultFont);
        lblSubscribeHumidity.setBounds(10, 91, 105, 20);
        pnlSubscribe.add(lblSubscribeHumidity);

        lblSubscribeHvac = new JLabel("HVAC Mode:");
        lblSubscribeHvac.setFont(defaultFont);
        lblSubscribeHvac.setBounds(10, 218, 80, 20);
        pnlSubscribe.add(lblSubscribeHvac);

        lblSubscribeAmbientResults = new JLabel("");
        lblSubscribeAmbientResults.setFont(boldFont);
        lblSubscribeAmbientResults.setBounds(133, 60, 47, 20);
        pnlSubscribe.add(lblSubscribeAmbientResults);

        lblSubscribeHumidityResults = new JLabel("");
        lblSubscribeHumidityResults.setFont(boldFont);
        lblSubscribeHumidityResults.setBounds(133, 91, 47, 20);
        pnlSubscribe.add(lblSubscribeHumidityResults);

        lblSubscribeTargetResults = new JLabel("");
        lblSubscribeTargetResults.setFont(boldFont);
        lblSubscribeTargetResults.setBounds(133, 122, 47, 20);
        pnlSubscribe.add(lblSubscribeTargetResults);

        lblSubscribeTargetHighResults = new JLabel("");
        lblSubscribeTargetHighResults.setFont(boldFont);
        lblSubscribeTargetHighResults.setBounds(133, 153, 47, 20);
        pnlSubscribe.add(lblSubscribeTargetHighResults);

        lblSubscribeTargetLowResults = new JLabel("");
        lblSubscribeTargetLowResults.setFont(boldFont);
        lblSubscribeTargetLowResults.setBounds(133, 184, 47, 20);
        pnlSubscribe.add(lblSubscribeTargetLowResults);

        lblSubscribeHvacResults = new JLabel("");
        lblSubscribeHvacResults.setHorizontalAlignment(SwingConstants.LEFT);
        lblSubscribeHvacResults.setFont(boldFont);
        lblSubscribeHvacResults.setBounds(133, 218, 126, 20);
        pnlSubscribe.add(lblSubscribeHvacResults);

        JLabel lblAwayHigh = new JLabel("Away High:");
        lblAwayHigh.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 14));
        lblAwayHigh.setBounds(264, 91, 94, 20);
        pnlSubscribe.add(lblAwayHigh);

        lblSubscribeAwayHighResults = new JLabel("");
        lblSubscribeAwayHighResults.setFont(new Font("Consolas", Font.BOLD, 18));
        lblSubscribeAwayHighResults.setBounds(368, 91, 47, 20);
        pnlSubscribe.add(lblSubscribeAwayHighResults);

        JLabel lblAwayLow = new JLabel("Away Low:");
        lblAwayLow.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 14));
        lblAwayLow.setBounds(264, 122, 94, 20);
        pnlSubscribe.add(lblAwayLow);

        lblSubscribeAwayLowResults = new JLabel("");
        lblSubscribeAwayLowResults.setFont(new Font("Consolas", Font.BOLD, 18));
        lblSubscribeAwayLowResults.setBounds(368, 122, 47, 20);
        pnlSubscribe.add(lblSubscribeAwayLowResults);

        JLabel lblHasLeaf = new JLabel("Has Leaf:");
        lblHasLeaf.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 14));
        lblHasLeaf.setBounds(264, 153, 94, 20);
        pnlSubscribe.add(lblHasLeaf);

        lblSubscribeHasLeafResults = new JLabel("");
        lblSubscribeHasLeafResults.setFont(new Font("Consolas", Font.BOLD, 18));
        lblSubscribeHasLeafResults.setBounds(368, 153, 63, 20);
        pnlSubscribe.add(lblSubscribeHasLeafResults);

        JLabel lblCanHeat = new JLabel("Can Heat:");
        lblCanHeat.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 14));
        lblCanHeat.setBounds(264, 184, 94, 20);
        pnlSubscribe.add(lblCanHeat);

        lblSubscribeCanHeatResults = new JLabel("");
        lblSubscribeCanHeatResults.setFont(new Font("Consolas", Font.BOLD, 18));
        lblSubscribeCanHeatResults.setBounds(368, 184, 63, 20);
        pnlSubscribe.add(lblSubscribeCanHeatResults);

        JLabel lblCanCool = new JLabel("Can Cool:");
        lblCanCool.setFont(new Font("Microsoft Sans Serif", Font.PLAIN, 14));
        lblCanCool.setBounds(264, 218, 94, 20);
        pnlSubscribe.add(lblCanCool);

        lblSubscribeCanCoolResults = new JLabel("");
        lblSubscribeCanCoolResults.setFont(new Font("Consolas", Font.BOLD, 18));
        lblSubscribeCanCoolResults.setBounds(368, 218, 63, 20);
        pnlSubscribe.add(lblSubscribeCanCoolResults);

        // *** Created by windowbuilder ***
        btnSetTargetHigh = new JButton("Target High");
        btnSetTargetHigh.setMargin(new Insets(0, 0, 0, 0));
        btnSetTargetHigh.setFont(defaultFont);
        btnSetTargetHigh.setEnabled(true);
        btnSetTargetHigh.addActionListener(actionListener);
        btnSetTargetHigh.setBounds(10, 68, 100, 28);
        pnlSet.add(btnSetTargetHigh);

        btnSetTargetLow = new JButton("Target Low");
        btnSetTargetLow.setMargin(new Insets(0, 0, 0, 0));
        btnSetTargetLow.setFont(defaultFont);
        btnSetTargetLow.setEnabled(true);
        btnSetTargetLow.addActionListener(actionListener);
        btnSetTargetLow.setBounds(10, 107, 100, 28);
        pnlSet.add(btnSetTargetLow);

        spinnerSetTargetHigh = new JSpinner();
        spinnerSetTargetHigh.setModel(new SpinnerNumberModel(new Integer(75), null, null, new Integer(1)));
        spinnerSetTargetHigh.setFont(defaultFont);
        spinnerSetTargetHigh.setBounds(140, 69, 51, 28);
        pnlSet.add(spinnerSetTargetHigh);

        spinnerSetTargetLow = new JSpinner();
        spinnerSetTargetLow.setModel(new SpinnerNumberModel(new Integer(68), null, null, new Integer(1)));
        spinnerSetTargetLow.setFont(defaultFont);
        spinnerSetTargetLow.setBounds(140, 108, 51, 28);
        pnlSet.add(spinnerSetTargetLow);

        // setup frame and add panels 
        this.getContentPane().setLayout(null);
        this.setBounds(80,30, 553, 800);
        this.setVisible(true);
        this.getContentPane().add(pnlRequestor);
        this.repaint();
    }

    public void setRequestor(Requestor req){
        this.requestor = req;
    }
    
    private void shutdown(){
        if(requestor != null)
            requestor.destroy();
        this.dispose();
    }

    // Display the results from various subscriptions

    public void displaySubscribeAmbientResults(String providerID, int value){
        if(providerID.equals(providerOID)){
            lblSubscribeAmbientResults.setText(String.valueOf(value));
        }
    }
    public void displaySubscribeTargetResults(String providerID, int target){
        if(providerID.equals(providerOID)){
            lblSubscribeTargetResults.setText(String.valueOf(target));
        }
    }
    public void displaySubscribeTargetHighResults(String providerID, int target){
        if(providerID.equals(providerOID))
            lblSubscribeTargetHighResults.setText(String.valueOf(target));
    }
    public void displaySubscribeTargetLowResults(String providerID, int target){
        if(providerID.equals(providerID))
            lblSubscribeTargetLowResults.setText(String.valueOf(target));
    }
    public void displaySubscribeHumidityResults(String providerID, int target){
        if(providerID.equals(providerID))
            lblSubscribeHumidityResults.setText(String.valueOf(target)+"%");
    }
    public void displaySubscribeAwayHigh(String providerID, int target){
        if(providerID.equals(providerID))
            lblSubscribeAwayHighResults.setText(String.valueOf(target));
    }
    public void displaySubscribeAwayLow(String providerID, int target){
        if(providerID.equals(providerID))
            lblSubscribeAwayLowResults.setText(String.valueOf(target));
    }
    public void displaySubscribeCanHeatResults(String providerID, boolean target){
        if(providerID.equals(providerID))
            lblSubscribeCanHeatResults.setText(String.valueOf(target));
    }
    public void displaySubscribeCanCoolResults(String providerID, boolean target){
        if(providerID.equals(providerID))
            lblSubscribeCanCoolResults.setText(String.valueOf(target));
    }
    public void displaySubscribeHasLeafResults(String providerID, boolean target){
        if(providerID.equals(providerID))
            lblSubscribeHasLeafResults.setText(String.valueOf(target));
    }
    public void displaySubscribeHvacResults(String providerID, String target){
        if(providerID.equals(providerID))
            lblSubscribeHvacResults.setText(target);
    }

    // For asynch get ops, display the GET results
    public void displayGetVersionResults(String providerID, String target){
        if(providerID.equals(providerID))
            lblGetVersionResults.setText(target);
    }

    public void displayGetAmbientResults(String providerID, int value)
    {
        if(providerID.equals(providerOID))
        {
            lblGetAmbientResults.setText(String.valueOf(value));
        }
        else
            System.out.println(providerID);
    }

    public void displayGetTargetResults(String providerID, int value)
    {
        if(providerID.equals(providerOID))
        {
            lblGetTargetResults.setText(String.valueOf(value));
        }
        else
            System.out.println(providerID);
    }

    public void displayGetHvacResults(String providerID, String value)
    {
        if(providerID.equals(providerOID))
        {
            lblGetHvacResults.setText(value);
        }
        else
            System.out.println(providerID);
    }

    public void displayGetTargetHighResults(String providerID, int value){
        if(providerID.equals(providerID))
            lblGetTargetHighResults.setText(String.valueOf(value));
        else
            System.out.println(providerID);
    }

    public void displayGetTargetLowResults(String providerID, int value){
        if(providerID.equals(providerID))
            lblGetTargetLowResults.setText(String.valueOf(value));
        else
            System.err.println(providerID);
    }

    public void displayGetHumidityResults(String providerID, int value){
        if(providerID.equals(providerID))
            lblGetHumidityResults.setText(String.valueOf(value)+"%");
        else
            System.err.println(providerID);
    }

    public void displayGetNameResults(String providerID, String name){
        if(providerID.equals(providerID))
            lblGetNameResults.setText(name);
        else
            System.err.println(providerID);
    }

    public void displayGetDeviceIDResults(String providerID, String id){
        if(providerID.equals(providerID))
            lblGetDeviceIDResults.setText(id);
        else
            System.err.println(providerID);
    }

    public void displayGetTempScaleResults(String providerID, String scale){
        if(providerID.equals(providerID))
            lblGetTempScaleResults.setText(scale);
        else
            System.err.println(providerID);
    }

    // Show a message if the SET operation was successful, or had an error
    public void displaySetResults(String successMsg){
        lblSetResults.setText(successMsg);
    }


    // Handle user events
    private class NestUIActionListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if(e.getSource().equals(btnGetOp))
            {
                requestor.sendBeginGetAllRequest();
            } 
            else if(e.getSource().equals(btnSetTarget)){
                requestor.sendBeginSetTargetRequest((int)spinnerSetTarget.getValue());
            }
            else if(e.getSource().equals(btnSetTargetHigh)){
                requestor.sendBeginSetTargetHighRequest((int)spinnerSetTargetHigh.getValue());
            }
            else if(e.getSource().equals(btnSetTargetLow)){
                requestor.sendBeginSetTargetLowRequest((int)spinnerSetTargetLow.getValue());
            }
            else if(e.getSource().equals(btnSetHvac)){
                requestor.sendBeginSetHvacRequest(hvacComboBox.getSelectedItem().toString());
            }
            else if (e.getSource().equals(btnSubscribeAmbientOp)){
                requestor.sendBeginSubscribeAmbientRequest();
            } 
            else if(e.getSource().equals(btnSubscribeAllOp)){
                requestor.sendBeginSubscribeAllRequest();
            }
        }
    }

    private class WindowCloseHandler implements WindowListener{

        @Override
        public void windowOpened(WindowEvent e) {}

        @Override
        public void windowIconified(WindowEvent e) {}

        @Override
        public void windowDeiconified(WindowEvent e) {}

        @Override
        public void windowDeactivated(WindowEvent e) {}

        @Override
        public void windowClosing(WindowEvent e) {
            shutdown();
        }

        @Override
        public void windowClosed(WindowEvent e) {
            shutdown();
        }

        @Override
        public void windowActivated(WindowEvent e) {}
    }
}

