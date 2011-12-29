package dev2dev.server;

import java.text.ParseException;
import java.util.Set;

import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
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
	private String callIdProxy;
	private boolean isRegisteredAtProxy = false;
	
	public UserAgentServer(SipLayer sipLayer) {
		this.sipLayer = sipLayer;
		sipLayer.addMessageProcessor(this);
		try {
			contactHeader = sipLayer.getContactHeader();		
			callIdProxy = sipLayer.register(PROXY_ADDRESS, PROXY_PORT);
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
	 * Handles incoming Bye requests and confirms with OK response
	 * 
	 * @param requestEvent
	 */
	@Override
	public void processBye(RequestEvent requestEvent) {
		// Proxy - UAS
		// BYE ->
		// <- 200 OK
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
	 * Handles incoming Invite requests
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
		

	}

	@Override
	public void processMessage(String sender, String message) {
		LOGGER.debug("processMessage()");
	}

	/**
	 * Handles incoming OK response and checks if registration was successful
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
