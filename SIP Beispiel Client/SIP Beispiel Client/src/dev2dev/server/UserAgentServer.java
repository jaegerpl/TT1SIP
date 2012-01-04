package dev2dev.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionUnavailableException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import dev2dev.igmp.IGMPSender;
import dev2dev.sip.MessageProcessor;
import dev2dev.sip.SipLayer;

public class UserAgentServer implements MessageProcessor {
	private static final Logger LOGGER = Logger.getLogger("UserAgentServer");	
	private static String PROXY_ADDRESS = "tiserver03.cpt.haw-hamburg.de";
	
	private SipLayer sipLayer;
	private ContactHeader contactHeader;
	private Set<String> activeDialogs;
	private Set<String> inactiveDialogs;
	private String callIdProxy;
	private boolean isRegisteredAtProxy = false;
	private IGMPSender igmpsender;
	
	public static void main(String[] args) {

		try {
			String username = "ToPa-Server";
			int port = 5070;
			String localHost = InetAddress.getLocalHost().getHostName();
			String ip = InetAddress.getByName(localHost).getHostAddress();

			// Starting UAS
			SipLayer sipLayer = new SipLayer(username, ip, port);
			UserAgentServer uas = new UserAgentServer(sipLayer);
			sipLayer.addMessageProcessor(uas);

			System.out.println("Server -"+username+"- h�rt auf Port: "+port);
		} catch (Throwable e) {
			System.out.println("Problem initializing the SIP stack.");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public UserAgentServer(SipLayer sipLay) {
		this.sipLayer = sipLay;
		igmpsender = new IGMPSender();
		activeDialogs = new HashSet<String>();
		try {
			igmpsender.initialize(InetAddress.getByName("239.238.237.17"), 9017, this);
			Thread t = new Thread(igmpsender);
			t.start();
			contactHeader = sipLayer.getContactHeader();		
			callIdProxy = sipLayer.register(PROXY_ADDRESS);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	

	/**
	 * Returns true if at least one SIP Client has established a session with
	 * the server.
	 * 
	 * @return true if at least one session exists, otherwise false
	 */
	public boolean sessionEstablished() {
		if (activeDialogs.size() != 0) return true;
		else return false;
	}

	/**
	 * Handles incoming Ack requests and adds caller to active dialogs
	 * 
	 * @param requestEvent
	 */
	@Override
	public void processAck(RequestEvent requestEvent) {
		// SIP implements a three-way handshake.
		// Proxy - UAS
		// INVITE ->
		// <- 200 OK
		// ACK ->
		LOGGER.debug("processAck()");
		String dialogId = requestEvent.getDialog().getDialogId();
		// sender is known and waiting to establish a dialog
		if (inactiveDialogs.contains(dialogId)) {
			LOGGER.debug(dialogId + " is now active!");
			activeDialogs.add(dialogId);
			inactiveDialogs.remove(dialogId);
		}
		// sender is unknown
		else {
			try {
				LOGGER.debug(dialogId + " is unknown!");
				Response response = sipLayer.createResponse(Response.DECLINE, requestEvent.getRequest());
				ServerTransaction serverTransaction = requestEvent.getServerTransaction();
				if (serverTransaction == null)
					serverTransaction = sipLayer.getNewServerTransaction(requestEvent.getRequest());
				serverTransaction.sendResponse(response);
				LOGGER.debug("Sent Decline: " + response.toString());
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handles incoming Bye requests and confirms with OK response
	 * 
	 * @param requestEvent
	 */
	@Override
	public void processBye(RequestEvent requestEvent) {
		// Proxy - UAS
		// BYE ->
		// <- 200 OK
		LOGGER.debug("processBye()");
		String dialogId = requestEvent.getDialog().getDialogId();
		// confirms BYE with OK
		if (activeDialogs.contains(dialogId)) {
			try {
				Response response = sipLayer.createResponse(Response.OK, requestEvent.getRequest());
				response.addHeader(contactHeader);
				ServerTransaction serverTransaction = requestEvent.getServerTransaction();
				if (serverTransaction == null) serverTransaction = sipLayer.getNewServerTransaction(requestEvent.getRequest());
				serverTransaction.sendResponse(response);
				LOGGER.debug("Sent OK: " + response.toString());
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handles incoming DialogTerminated and removes caller from active dialogs
	 * 
	 * @param dialogTerminatedEvent
	 */
	@Override
	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
		LOGGER.debug("processDialogTerminated()");
		String dialogId = dialogTerminatedEvent.getDialog().getDialogId();
		activeDialogs.remove(dialogId);
	}

	@Override
	public void processError(String error) {
		LOGGER.debug("processError()");
	}

	@Override
	public void processInfo(String info) {
		LOGGER.debug("processInfo()");
	}

	/**
	 * Handles incoming Invite requests with responses and adding the dialogs to the waiting list
	 * 
	 * @param requestEvent
	 */
	@Override
	public void processInvite(RequestEvent requestEvent) {
		// Proxy - UAS
		// Invite ->
		// <- 100 Trying
		// <- 180 Ringing
		// <- 200 OK
		// ACK ->
		LOGGER.debug("processInvite()");
		ServerTransaction serverTransaction = requestEvent.getServerTransaction();
		if (serverTransaction == null)
			try {
				serverTransaction = sipLayer.getNewServerTransaction(requestEvent.getRequest());
			} catch (TransactionAlreadyExistsException e1) {
				e1.printStackTrace();
			} catch (TransactionUnavailableException e1) {
				e1.printStackTrace();
			}
		// if not registered at the proxy decline his invite
		if (isRegisteredAtProxy == false) {
			LOGGER.debug("Declining invite from unregistered proxy!");
			try {
			Response response = sipLayer.createResponse(Response.DECLINE, requestEvent.getRequest());
			response.addHeader(contactHeader);			
			serverTransaction.sendResponse(response);
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
		}		
		else {
			try {
				// send ok				
				Response ok = sipLayer.createResponse(Response.OK, requestEvent.getRequest());
				ok.addHeader(contactHeader);
				serverTransaction.sendResponse(ok);
				LOGGER.debug("Sent OK: " + ok.toString());
				// wait for ACK
				String dialogId = serverTransaction.getDialog().getDialogId();
				inactiveDialogs.add(dialogId);
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		}

	}

	@Override
	public void processMessage(String sender, String message) {
		LOGGER.debug("processMessage()");
	}

	/**
	 * Handles incoming OK responses and checks if registration was successful
	 * 
	 * @param responseEvent
	 */
	@Override
	public void processOK(ResponseEvent responseEvent) {
		// UAS - Proxy
		// REGISTER ->
		// <- 200 OK
		LOGGER.debug("processOK()");
		String responseId = responseEvent.getDialog().getDialogId();
		LOGGER.debug("DialogId: " + responseId);
		CallIdHeader callIdHeader = (CallIdHeader) responseEvent.getResponse().getHeader(CallIdHeader.NAME);
		String callIdResponse = callIdHeader.getCallId();
		LOGGER.debug("CallId: " + callIdResponse);
		// not registered and callId is the one from the registration at the proxy
		if (isRegisteredAtProxy == false && callIdProxy.equals(callIdResponse)) {
			LOGGER.debug("Registration successful!");
			isRegisteredAtProxy = true;
		}		
	}

	@Override
	public void processRinging() {
		LOGGER.debug("processRinging()");
	}

	@Override
	public void processTrying() {
		LOGGER.debug("processTrying()");
	}
}
