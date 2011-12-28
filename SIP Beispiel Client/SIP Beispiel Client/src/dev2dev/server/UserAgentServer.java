package dev2dev.server;

import javax.sip.DialogTerminatedEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;

import dev2dev.sip.MessageProcessor;

public class UserAgentServer implements MessageProcessor {

	/**
	 * Returns true if at least one SIP Client has established a session with
	 * the server.
	 * 
	 * @return true if at least one session exists, otherwise false
	 */
	public boolean sessionEstablished() {
		return false;
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
