package dev2dev.textclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import dev2dev.igmp.IGMPListener;
import dev2dev.sip.MessageProcessor;
import dev2dev.sip.SipLayer;

public class TextClient extends JFrame implements MessageProcessor {

	private static final Logger LOGGER = Logger.getLogger("TextClient");

	private SipLayer sipLayer;

	// GUI STUFF
	private JTextField fromAddress;
	private JLabel fromLbl;
	private JLabel receivedLbl;
	private JTextArea receivedMessages;
	private JScrollPane receivedScrollPane;
	private JButton connectServerButton;
	private JButton joinIGMPButton;
	private JTextField toAddress;
	private JLabel toLbl;

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
		joinIGMPButton = new JButton();

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

		connectServerButton.setText("Con2Server");
		connectServerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				callBtnActionPerformed(evt);
			}
		});

		getContentPane().add(connectServerButton);
		connectServerButton.setBounds(1, 255, 100, 25);

		joinIGMPButton.setText("JoinGroup");
		joinIGMPButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				IGMPJoinButtonActionPerformed(evt);
			}
		});

		getContentPane().add(joinIGMPButton);
		joinIGMPButton.setBounds(102, 255, 100, 25);

		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		setBounds((screenSize.width - 288) / 2, (screenSize.height - 310) / 2,
				288, 320);
	}

//	private void sendBtnActionPerformed(ActionEvent evt) {
//
//		try {
//			String to = this.toAddress.getText();
//			String message = this.sendMessages.getText();
//			sipLayer.sendMessage(to, message);
//		} catch (Throwable e) {
//			e.printStackTrace();
//			this.receivedMessages.append("ERROR sending message: "
//					+ e.getMessage() + "\n");
//		}
//	}

	/**
	 * When pressing "Call" the TextClient will join an IGMP Group and send an
	 * invite to a server via the proxy.
	 * 
	 * When pressing "Hang Up" the TextClient will send a BYE to the server but
	 * will stay registered in the IGMP Group.
	 * 
	 * @param evt
	 */
	private void callBtnActionPerformed(ActionEvent evt) {

		// Calling (INVITE)
		if (connectServerButton.getText() == "Call") {
			System.out.println("Calling...");
			String to = this.toAddress.getText();
			try {
				connectServerButton.setText("Hang up");
				serverDialog = sipLayer.startCall(to);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
			System.out.println("Done. Call established");
			// Hanging UP (BYE)
		} else if (connectServerButton.getText() == "Hang up") {
			connectServerButton.setText("Call");
			System.out.println("Hanging up...");
			sipLayer.hangUp(serverDialog);
			System.out.println("Done hanging up.");
		} else {
			System.out.println("Fehler im Call-HangUp-Handling");
		}

	}

	/**
	 * When pressing "JoinGroup" the client connects to the specified MC Group.
	 * 
	 * When pressing "LeaveGroup" the client disconnects from the MC Group.
	 * 
	 * @param evt
	 */
	private void IGMPJoinButtonActionPerformed(ActionEvent evt) {
		if (joinIGMPButton.getText() == "JoinGroup") {
			System.out.println("Joining Group...");
			// Join IGMP Group
			try {
				igmpListener.initialize(
						InetAddress.getByName("239.238.237.17"), 9017, this);
				Thread t = new Thread(igmpListener);
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
		LOGGER.debug("processOK()");
	}

	public void processRinging() {
		LOGGER.debug("processRinging()");
	}

	public void processTrying() {
		LOGGER.debug("processTrying()");
	}
}
