package dev2dev.server;

import java.text.ParseException;
import java.util.Set;

import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.ContactHeader;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import dev2dev.sip.MessageProcessor;
import dev2dev.sip.SipLayer;

public class UserAgentServer implements MessageProcessor {
	private static final Logger LOGGER = Logger.getLogger("UserAgentServer");
	
	private static String PROXY_ADDRESS = "tiserver03.cpt.haw-hamburg.de";
	private static int PROXY_PORT = 5060; 
	
	private SipLayer sipLayer;
	private ContactHeader contactHeader;
	private Set<String> activeDialogs;
	private Set<String> inactiveDialogs;
	
	public UserAgentServer(SipLayer sipLayer) {
		this.sipLayer = sipLayer;
		sipLayer.setMessageProcessor(this);
		try {
			contactHeader = sipLayer.getContactHeader();		
			sipLayer.register(PROXY_ADDRESS, PROXY_PORT);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
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
	 * Handles incoming Ack requests
	 * 
	 * @param requestEvent
	 */
	@Override
	public void processAck(RequestEvent requestEvent) {
		// SIP method ack
		// SIP implements a three-way handshake.
		// The caller sends an INVITE
		// The callee sends an 200 OK to accept the call
		// The caller sends an ACK to indicate that the handshake is done and a call is going to be setup
		
		String dialogId = requestEvent.getDialog().getDialogId();
		// sender is known and waiting to establish a dialog
		if (inactiveDialogs.contains(dialogId)) {
			activeDialogs.add(dialogId);
			inactiveDialogs.remove(dialogId);
		}
		// sender is unknown
		else {
			try {
				Response response = sipLayer.createResponse(Response.DECLINE, requestEvent.getRequest());
				sipLayer.sendResponse(response);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handles incoming Bye requests
	 * 
	 * @param requestEvent
	 */
	@Override
	public void processBye(RequestEvent requestEvent) {
		String dialogId = requestEvent.getDialog().getDialogId();
		// confirms BYE with OK
		if (activeDialogs.contains(dialogId)) {
			try {
				Response response = sipLayer.createResponse(Response.OK, requestEvent.getRequest());
				response.addHeader(contactHeader);
				ServerTransaction serverTransaction = requestEvent.getServerTransaction();
				serverTransaction.sendResponse(response);
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent dte) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processError(String error) {
		LOGGER.debug("processError()");
	}

	@Override
	public void processInfo(String info) {
		LOGGER.debug("processInfo()");
	}

	@Override
	public void processInvite(RequestEvent requestEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processMessage(String sender, String message) {
		LOGGER.debug("processMessage()");
	}

	@Override
	public void processOK(ResponseEvent responseEvent) {
		// TODO Auto-generated method stub

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
