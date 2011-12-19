package dev2dev.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionDoesNotExistException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;


public class SipLayer implements SipListener {

	private MessageProcessor messageProcessor;

	private String username;

	private SipStack sipStack;

	private SipFactory sipFactory;

	private AddressFactory addressFactory;

	private HeaderFactory headerFactory;

	private MessageFactory messageFactory;

	private SipProvider sipProvider;

	// Headers and URIs
	private SipURI fromURI; // this Clients URI
	private String toUsername; // target Username
	private String toAddress; // target Address
	private SipURI toURI; // consists of toUsername and toAddress
	private SipURI requestURI;
	private SipURI contactURI;
	private FromHeader fromHeader;
	private ToHeader toHeader;
	private ArrayList<ViaHeader> viaHeaders;
	private CSeqHeader cSeqHeader;
	private CallIdHeader callIdHeader;
	private MaxForwardsHeader maxForwards;
	private ContentTypeHeader contentTypeHeader;

	/**
	 * Here we initialize the SIP stack.
	 * 
	 * @throws SipException
	 * @throws ParseException
	 */
	public SipLayer(String username, String ip, int port)
			throws InvalidArgumentException, TooManyListenersException,
			ParseException, SipException {
		this.username = username;
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "TextClient");
		properties.setProperty("javax.sip.IP_ADDRESS", ip);

		// DEBUGGING: Information will go to files
		// textclient.log and textclientdebug.log
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
				"textclient.txt");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
				"textclientdebug.log");

		sipStack = sipFactory.createSipStack(properties);
		headerFactory = sipFactory.createHeaderFactory();
		addressFactory = sipFactory.createAddressFactory();
		messageFactory = sipFactory.createMessageFactory();

		@SuppressWarnings("deprecation")
		ListeningPoint tcp = sipStack.createListeningPoint(port, "tcp");
		ListeningPoint udp = sipStack.createListeningPoint(port, "udp");

		sipProvider = sipStack.createSipProvider(tcp);
		sipProvider.addSipListener(this);
		sipProvider = sipStack.createSipProvider(udp);
		sipProvider.addSipListener(this);

		register("tiserver03.cpt.haw-hamburg.de", 5060);
	}

	/**
	 * This method uses the SIP stack to send a message.
	 */
	public void sendMessage(String to, String message) throws ParseException,
			InvalidArgumentException, SipException {

		setupHeaders(to);

		// Putting REQUEST together
		Request request = messageFactory.createRequest(requestURI,
				Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		// Adding ContactHeader to REQUEST
		contactURI = addressFactory.createSipURI(getUsername(), getHost());
		contactURI.setPort(getPort());
		Address contactAddress = addressFactory.createAddress(contactURI);
		contactAddress.setDisplayName(getUsername());
		ContactHeader contactHeader = headerFactory
				.createContactHeader(contactAddress);
		request.addHeader(contactHeader);

		// Adding MESSAGE to REQUEST
		request.setContent(message, contentTypeHeader);

		// Send REQUEST
		sipProvider.sendRequest(request);
	}

	/** This method is called by the SIP stack when a response arrives. */
	public void processResponse(ResponseEvent evt) {
		Response response = evt.getResponse();
		int status = response.getStatusCode();

		if ((status >= 200) && (status < 300)) { // Success!
			messageProcessor.processInfo("--Sent");
			return;
		}

		messageProcessor.processError("Previous message not sent: " + status);
	}

	/**
	 * This method is called by the SIP stack when a new request arrives.
	 */
	public void processRequest(RequestEvent evt) {
		Request req = evt.getRequest();

		String method = req.getMethod();
		if (!method.equals("MESSAGE")) { // bad request type.
			messageProcessor.processError("Bad request type: " + method);
			return;
		}

		FromHeader from = (FromHeader) req.getHeader("From");
		messageProcessor.processMessage(from.getAddress().toString(),
				new String(req.getRawContent()));
		Response response = null;
		try { // Reply with OK
			response = messageFactory.createResponse(200, req);
			ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
			toHeader.setTag("888"); // This is mandatory as per the spec.
			ServerTransaction st = sipProvider.getNewServerTransaction(req);
			st.sendResponse(response);
		} catch (Throwable e) {
			e.printStackTrace();
			messageProcessor.processError("Can't send OK reply.");
		}
	}

	/**
	 * This method is called by the SIP stack when there's no answer to a
	 * message. Note that this is treated differently from an error message.
	 */
	public void processTimeout(TimeoutEvent evt) {
		messageProcessor
				.processError("Previous message not sent: " + "timeout");
	}

	/**
	 * This method is called by the SIP stack when there's an asynchronous
	 * message transmission error.
	 */
	public void processIOException(IOExceptionEvent evt) {
		messageProcessor.processError("Previous message not sent: "
				+ "I/O Exception");
	}

	/**
	 * This method is called by the SIP stack when a dialog (session) ends.
	 */
	public void processDialogTerminated(DialogTerminatedEvent evt) {
	}

	/**
	 * This method is called by the SIP stack when a transaction ends.
	 */
	public void processTransactionTerminated(TransactionTerminatedEvent evt) {
	}

	public String getHost() {
		int port = sipProvider.getListeningPoint().getPort();
		String host = sipStack.getIPAddress();
		return host;
	}

	public int getPort() {
		int port = sipProvider.getListeningPoint().getPort();
		return port;
	}

	public String getUsername() {
		return username;
	}

	public MessageProcessor getMessageProcessor() {
		return messageProcessor;
	}

	public void setMessageProcessor(MessageProcessor newMessageProcessor) {
		messageProcessor = newMessageProcessor;
	}

	public Dialog startCall(String to) throws ParseException,
			InvalidArgumentException, SipException {

		// Create and send INVITE Request
		setupHeaders(to);

		Request invite = messageFactory.createRequest("INVITE " + to
				+ " SIP/2.0\n");
		invite.addHeader(sipProvider.getNewCallId());
		invite.addHeader(headerFactory.createCSeqHeader(1l, Request.INVITE));
		fromURI = addressFactory.createSipURI(getUsername(), getHost() + ":"
				+ getPort());
		invite.addHeader(headerFactory.createFromHeader(
				addressFactory.createAddress(fromURI), "tag"));
		invite.addHeader((ViaHeader) viaHeaders.get(0));
		invite.addHeader(toHeader);
		invite.addHeader(maxForwards);
		contactURI = addressFactory.createSipURI(getUsername(), getHost());
		contactURI.setPort(getPort());
		Address contactAddress = addressFactory.createAddress(contactURI);
		contactAddress.setDisplayName(getUsername());
		ContactHeader contactHeader = headerFactory
				.createContactHeader(contactAddress);
		invite.addHeader(contactHeader);

		// Start Transaction
		ClientTransaction trans;

		trans = sipProvider.getNewClientTransaction(invite);
		Dialog dia = trans.getDialog();
		trans.sendRequest();
		return dia;
	}

	/**
	 * Registers the Server with the SIP Proxy
	 * 
	 * @param proxyAddress
	 * @param proxyPort
	 * @return
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public String register(String proxyAddress, int proxyPort)
			throws ParseException, InvalidArgumentException, SipException {
		SipURI from = addressFactory.createSipURI(getUsername(), getHost()
				+ ":" + getPort());
		Address fromNameAddress = addressFactory.createAddress(from);
		fromNameAddress.setDisplayName(getUsername());
		FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
				null);
		ToHeader toHeader = headerFactory.createToHeader(fromNameAddress, null);
		SipURI requestURI = addressFactory.createSipURI(toUsername,
				proxyAddress);
		requestURI.setTransportParam("udp");
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = headerFactory.createViaHeader(getHost(),
				getPort(), "udp", null);
		viaHeaders.add(viaHeader);
		CallIdHeader callIdHeader = sipProvider.getNewCallId();
		long sequenceNumber = 1;
		CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequenceNumber,
				Request.REGISTER);
		MaxForwardsHeader maxForwards = headerFactory
				.createMaxForwardsHeader(70);

		Request request = messageFactory.createRequest(requestURI,
				Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);

		SipURI contactURI = addressFactory.createSipURI(getUsername(),
				getHost());
		contactURI.setPort(getPort());
		Address contactAddress = addressFactory.createAddress(contactURI);
		contactAddress.setDisplayName(getUsername());

		ContactHeader contactHeader = headerFactory
				.createContactHeader(contactAddress);
		request.addHeader(contactHeader);

		sipProvider.sendRequest(request);
		return callIdHeader.getCallId();
	}	

	private void setupHeaders(String to) throws ParseException,
			InvalidArgumentException {

		// Setting fromURI and FromHeader
		fromURI = addressFactory.createSipURI(getUsername(), getHost() + ":"
				+ getPort());
		Address fromNameAddress = addressFactory.createAddress(fromURI);
		fromNameAddress.setDisplayName(getUsername());
		fromHeader = headerFactory.createFromHeader(fromNameAddress,
				"textclientv1.0");

		// Setting toURI and ToHeader
		toUsername = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
		toAddress = to.substring(to.indexOf("@") + 1);

		toURI = addressFactory.createSipURI(toUsername, toAddress);
		Address toNameAddress = addressFactory.createAddress(toURI);
		toNameAddress.setDisplayName(toUsername);
		toHeader = headerFactory.createToHeader(toNameAddress, null);

		// Setting RequestURI
		requestURI = addressFactory.createSipURI(toUsername, toAddress);
		requestURI.setTransportParam("udp");

		// Setting viaHeaders
		viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = headerFactory.createViaHeader(getHost(),
				getPort(), "udp", "branch1");
		viaHeaders.add(viaHeader);

		callIdHeader = sipProvider.getNewCallId();

		cSeqHeader = headerFactory.createCSeqHeader(1l, Request.MESSAGE);

		maxForwards = headerFactory.createMaxForwardsHeader(70);

		contentTypeHeader = headerFactory.createContentTypeHeader("text",
				"plain");
	}

	public void hangUp(Dialog serverDialog) {
		Request bye;
		try {
			bye = serverDialog.createRequest(Request.BYE);
			serverDialog.sendRequest(this.sipProvider
					.getNewClientTransaction(bye));
		} catch (TransactionDoesNotExistException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransactionUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
