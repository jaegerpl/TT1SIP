package sip;

import javax.sip.DialogTerminatedEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;

public interface MessageProcessor {
	public void processAck(RequestEvent requestEvent);

	public void processBye(RequestEvent requestEvent);

	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent);

	public void processError(String error);

	public void processInfo(String info);

	public void processInvite(RequestEvent requestEvent);

	public void processMessage(String sender, String message);

	public void processOK(ResponseEvent responseEvent);

	public void processRinging();

	public void processTrying();
}
