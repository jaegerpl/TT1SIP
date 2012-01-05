package textclient;

import igmp.IGMPListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import org.apache.log4j.Logger;

import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import sip.MessageProcessor;
import sip.SipLayer;

public class TextClient extends JFrame implements MessageProcessor, TableModelListener {

	private static final Logger LOGGER = Logger.getLogger(TextClient.class);

	private SipLayer sipLayer;
	
	ClientControll dataModel;

	// GUI STUFF
	private JTextField fromAddress;
	private JLabel fromLbl;
	private JLabel receivedLbl;
	private JTextArea receivedMessages;
	private JScrollPane receivedScrollPane;
	private JButton connectServerButton;
	private JButton disconnectServerButton;
	private JButton joinIGMPButton;
	private JTextField toAddress;
	private JLabel toLbl;
	private JTable serverTable;

	// SIP STUFF
	private Dialog serverDialog; // the dialog received when sending INVITE to a
									// server

	// IGMP STUFF
	static IGMPListener igmpListener;

	public static void main(String[] args) {
		if (args.length != 2) {
			printUsage();
			System.exit(-1);
		}

		try {
			String username = args[0];
			int port = Integer.parseInt(args[1]);
			String localHost = InetAddress.getLocalHost().getHostName();
			String ip = InetAddress.getByName(localHost).getHostAddress();

			igmpListener = new IGMPListener();

			// Starting TextClient
			SipLayer sipLayer = new SipLayer(username, ip, port);
			TextClient tc = new TextClient(sipLayer);
			sipLayer.addMessageProcessor(tc);

			tc.show();
		} catch (Throwable e) {
			System.out.println("Problem initializing the SIP stack.");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void printUsage() {
		System.out.println("Syntax:");
		System.out.println("  java -jar textclient.jar <username> <port>");
		System.out.println("where <username> is the nickname of this user");
		System.out
				.println("and <port> is the port number to use. Usually 5060 if not used by another process.");
		System.out.println("Example:");
		System.out.println("  java -jar textclient.jar snoopy71 5061");
	}

	public TextClient(SipLayer sip) {
		super();
		sipLayer = sip;
		initWindow();
		String from = "sip:" + sip.getUsername() + "@" + sip.getHost() + ":"
				+ sip.getPort();
		this.fromAddress.setText(from);
		dataModel = new ClientControll(this);
		serverTable = new JTable(dataModel);
		serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		serverTable.getModel().addTableModelListener(this);
		serverTable.getSelectionModel().addListSelectionListener(new RowListener());
	}

	private void initWindow() {
		receivedLbl = new JLabel();
		receivedScrollPane = new JScrollPane();
		receivedMessages = new JTextArea();
		fromLbl = new JLabel();
		fromAddress = new JTextField();
		toLbl = new JLabel();
		toAddress = new JTextField();
		connectServerButton = new JButton();
		disconnectServerButton = new JButton();
		joinIGMPButton = new JButton();
		JScrollPane scrollpane = new JScrollPane(serverTable);

		getContentPane().setLayout(null);

		setTitle("TextClient");
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});

		receivedLbl.setText("Received Messages:");
		receivedLbl.setAlignmentY(0.0F);
		receivedLbl.setPreferredSize(new java.awt.Dimension(25, 100));
		getContentPane().add(receivedLbl);
		receivedLbl.setBounds(5, 0, 136, 20);

		receivedMessages.setAlignmentX(0.0F);
		receivedMessages.setEditable(false);
		receivedMessages.setLineWrap(true);
		receivedMessages.setWrapStyleWord(true);
		receivedScrollPane.setViewportView(receivedMessages);
		receivedScrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		getContentPane().add(receivedScrollPane);
		receivedScrollPane.setBounds(5, 20, 270, 130);

		fromLbl.setText("From:");
		getContentPane().add(fromLbl);
		fromLbl.setBounds(5, 151, 35, 15);

		getContentPane().add(fromAddress);
		fromAddress.setBounds(40, 151, 235, 20);
		fromAddress.setEditable(false);

		toLbl.setText("Join:");
		getContentPane().add(toLbl);
		toLbl.setBounds(5, 175, 35, 15);

		getContentPane().add(toAddress);
		toAddress.setBounds(40, 175, 235, 21);
		
		getContentPane().add(scrollpane);
		scrollpane.setBounds(5, 200, 270, 200);

		connectServerButton.setText("Con2Server");
		connectServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				connectServer();
			}
		});

		disconnectServerButton.setText("Discon. Server");
		disconnectServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				disconnectServer();
			}
		});
		
		getContentPane().add(connectServerButton);
		connectServerButton.setBounds(1, 405, 150, 25);

		getContentPane().add(disconnectServerButton);
		disconnectServerButton.setBounds(1, 435, 150, 25);
		
		joinIGMPButton.setText("JoinGroup");
		joinIGMPButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				joinIGMPGroup();
			}
		});

		getContentPane().add(joinIGMPButton);
		joinIGMPButton.setBounds(152, 405, 130, 25);

		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		setBounds((screenSize.width - 288) / 2, (screenSize.height - 500) / 2,
				288, 500);
	}

	/**
	 * Disconnect from the server displayed in the toAddress field and then remove 
	 * from the list of servers in the gui
	 */
	private void disconnectServer() {		
		Dialog dialog;
		String server = toAddress.getText();
		if(server.equals("")){
			LOGGER.debug("Kein Server ausgewŠhlt zum disconnecten");
		}
		else {
			dialog = dataModel.getServerDialog(server);			
			sipLayer.hangUp(dialog);			
			LOGGER.debug("Sending BYE to server:"+dialog.getDialogId());
		}
	}

	/**
	 * When pressing "Call" the TextClient will join an IGMP Group and send an
	 * invite to a server via the proxy.
	 * 
	 * When pressing "Hang Up" the TextClient will send a BYE to the server but
	 * will stay registered in the IGMP Group.
	 * 
	 * @param evt
	 */
	private void connectServer() {

		// Calling (INVITE)
		if (connectServerButton.getText() == "Con2Server" &&
				!(toAddress.getText().equals(""))) {
			LOGGER.info("Connecting to Server...");
			String to = this.toAddress.getText();
			try {
				serverDialog = sipLayer.startCall(to);
				// if server successfully joined, clean GUI
				if(serverDialog != null){
					dataModel.addServer(serverDialog, to);
					toAddress.setText("");
				}
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
		} else {
			LOGGER.info("Kein Server zum connecten ausgewŠhlt.");
		}
	}

	/**
	 * When pressing "JoinGroup" the client connects to the specified MC Group.
	 * 
	 * When pressing "LeaveGroup" the client disconnects from the MC Group.
	 * 
	 * @param evt
	 */
	private void joinIGMPGroup() {
		if (joinIGMPButton.getText() == "JoinGroup") {
			System.out.println("Joining Group...");
			// Join IGMP Group
			try {
				igmpListener.initialize(
						InetAddress.getByName("239.238.237.17"), 9017, this);
				Thread t = new Thread(igmpListener);
				t.start();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			joinIGMPButton.setText("LeaveGroup");
			System.out.println("Done. Group Joined");
		} else if (joinIGMPButton.getText() == "LeaveGroup") {
			System.out.println("Leaving Group.");
			igmpListener.stop();
			joinIGMPButton.setText("JoinGroup");
			System.out.println("Done leavig Group.");
		}
	}

	public void processMessage(String sender, String message) {
		this.receivedMessages.append("From " + sender + ": " + message + "\n");
	}

	public void processAck(RequestEvent requestEvent) {
		LOGGER.debug("processAck()");
	}

	public void processBye(RequestEvent requestEvent) {
		LOGGER.debug("processBye()");
	}

	public void processDialogTerminated(DialogTerminatedEvent dte) {
		LOGGER.debug("processDialogTerminated()");
	}

	public void processError(String errorMessage) {
		this.receivedMessages.append("ERROR: " + errorMessage + "\n");
	}

	public void processInfo(String infoMessage) {
		this.receivedMessages.append(infoMessage + "\n");
	}

	public void processInvite(RequestEvent requestEvent) {
		LOGGER.debug("processInvite()");
	}

	public void processOK(ResponseEvent responseEvent) {
		// OK Message means 
		// 1 - the connection to the server is established
		//     Sent ACK as reply
		// 2 - BYE Request has been understood
		//     remove server from list
		
		Response respones = responseEvent.getResponse();
		CSeqHeader cseq = (CSeqHeader) respones.getHeader(CSeqHeader.NAME);
		Dialog dia = responseEvent.getDialog();
		
		// 1 
		// INVITE -> 
		// OK     <-
		if (cseq.getMethod().equals(Request.INVITE)) {
			Request ackRequest;
			LOGGER.debug("Receiving OK after INVITE");			
			try {
				ackRequest = dia.createRequest(Request.ACK);
				dia.sendAck(ackRequest);
			} catch (SipException e) {			
				e.printStackTrace();
			} 	
			
			dataModel.markServerAsConnected(dia);
			LOGGER.debug("processOK() after INVITE");
		}		
		
		// 2 
		// BYE -> 
		// OK  <-
		if (cseq.getMethod().equals(Request.BYE)) {
			dataModel.disconnectServer(dia);		
			LOGGER.debug("Receiving OK after BYE from server"+ dia);		
					
			dataModel.removeServer(dia);
			toAddress.setText("");
			LOGGER.debug("processOK() after BYE");
		}
	}

	public void processRinging() {
		LOGGER.debug("processRinging()");
	}

	public void processTrying() {
		LOGGER.debug("processTrying()");
	}
    
    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }            
            serverTable.getSelectionModel().getLeadSelectionIndex();
            serverTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
     
        int[] rows = serverTable.getSelectedRows();
        toAddress.setText(serverTable.getValueAt(rows[0], 0).toString());
        }
    }

	@Override
	public void tableChanged(TableModelEvent e) {
		serverTable.repaint();
		getContentPane().repaint();
		System.out.println("Neu zeichnen");
	}
}
