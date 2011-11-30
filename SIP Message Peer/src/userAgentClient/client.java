package userAgentClient;

import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransportNotSupportedException;

public class client {
	
	SipFactory sipFactory = null;
	SipStack sipStack = null;
	ListeningPoint lp = null;
	SipListener listener = null;
	
	public void client(){
		// Eigenschaften des SIP Stacks definieren
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "shootme"); 
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32"); 
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","debug"); 
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG","srvlog");
		// Erzeugen
		sipFactory = SipFactory.getInstance(); sipFactory.setPathName("gov.nist");
		// SipStack: Pfad +".javax.sip.SipStackImpl" 
		try {
			sipStack = sipFactory.createSipStack(properties);
		} catch (PeerUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// ListeningPoint
		try {
			lp = sipStack.createListeningPoint("127.0.0.1",5070, "udp");
		} catch (TransportNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		SipProvider sipProvider;
		try {
			sipProvider = sipStack.createSipProvider(lp);
			try {
				sipProvider.addSipListener(listener);
			} catch (TooManyListenersException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ObjectInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

	
	
}
