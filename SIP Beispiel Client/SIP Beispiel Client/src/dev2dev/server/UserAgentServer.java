package dev2dev.server;

import java.text.ParseException;
import java.util.Set;

import javax.sip.DialogTerminatedEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.ContactHeader;

import dev2dev.sip.MessageProcessor;
import dev2dev.sip.SipLayer;

public class UserAgentServer implements MessageProcessor {
	
	private static String PROXY_ADDRESS = "tiserver03.cpt.haw-hamburg.de";
	private static int PROXY_PORT = 5060; 
	
	private SipLayer sipLayer;
	private ContactHeader contactHeader;
	private Set<String> activeDialogs;
	
	public UserAgentServer(SipLayer sipLayer) {
		this.sipLayer = sipLayer;
		sipLayer.setMessageProcessor(this);
		try {
			contactHeader = sipLayer.getContactHeader();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
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

	@Override
	public void processAck(RequestEvent requestEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processBye(RequestEvent requestEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent dte) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processError(String error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processInfo(String info) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processInvite(RequestEvent requestEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processMessage(String sender, String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processOK(ResponseEvent responseEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processRinging() {
		// TODO Auto-generated method stub

	}

	@Override
	public void processTrying() {
		// TODO Auto-generated method stub

	}
}
