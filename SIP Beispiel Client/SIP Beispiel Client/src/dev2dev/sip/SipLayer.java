package dev2dev.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
import javax.sip.TransactionAlreadyExistsException;
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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class SipLayer implements SipListener {
	private static final Logger LOGGER = Logger.getLogger("SIPLayer");

	private List<MessageProcessor> messageProcessors;

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
	public SipLayer(String username, String ip, int port) throws InvalidArgumentException, TooManyListenersException,
			ParseException, SipException {
		LOGGER.addAppender(new ConsoleAppender(new SimpleLayout()));
		LOGGER.setLevel(Level.ALL);
		this.username = username;
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "TextClient");
		properties.setProperty("javax.sip.IP_ADDRESS", ip);

		// DEBUGGING: Information will go to files
		// textclient.log and textclientdebug.log
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "textclient.txt");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "textclientdebug.log");

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

		messageProcessors = new ArrayList<MessageProcessor>();
	}

	/**
	 * This method uses the SIP stack to send a message.
	 * 
	 * @param to
	 *            the server's address
	 * @param message
	 *            the message itseld
	 * 
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public void sendMessage(String to, String message) throws ParseException, InvalidArgumentException, SipException {
		Request request = messageFactory.createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);

		setupHeaders(request, Request.MESSAGE, to);

		// Adding MESSAGE to REQUEST
		request.setContent(message, contentTypeHeader);

		// Send REQUEST
		sipProvider.sendRequest(request);
	}

	/**
	 * Sends an invite message to a server. Server-Address is taken from to "to"
	 * field of the gui
	 * 
	 * @param to
	 *            the server's address
	 * 
	 * @return the dialog of the communication
	 * 
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public Dialog startCall(String to) throws ParseException, InvalidArgumentException, SipException {

		Request invite = messageFactory.createRequest("INVITE " + to + " SIP/2.0\n");
		setupHeaders(invite, Request.INVITE, to);

		// Start Transaction
		ClientTransaction trans;

		trans = sipProvider.getNewClientTransaction(invite);
		Dialog dia = trans.getDialog();
		trans.sendRequest();
		return dia;
	}

	/**
	 * Sets up and adds the message header for a given request
	 * 
	 * @param req
	 *            the request to which the headers are added to
	 * @param requestType
	 *            the type of the request
	 * @param to
	 *            the server's address
	 * 
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	private void setupHeaders(Request req, String requestType, String to) throws ParseException,
			InvalidArgumentException {

		// Setting fromURI and FromHeader
		fromURI = addressFactory.createSipURI(getUsername(), getHost() + ":" + getPort());
		Address fromNameAddress = addressFactory.createAddress(fromURI);
		fromNameAddress.setDisplayName(getUsername());
		fromHeader = headerFactory.createFromHeader(fromNameAddress, "textclientv1.0");
		req.addHeader(fromHeader);

		// Setting toURI and ToHeader
		toUsername = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
		toAddress = to.substring(to.indexOf("@") + 1);

		toURI = addressFactory.createSipURI(toUsername, toAddress);
		Address toNameAddress = addressFactory.createAddress(toURI);
		toNameAddress.setDisplayName(toUsername);
		toHeader = headerFactory.createToHeader(toNameAddress, null);
		req.addHeader(toHeader);

		// Setting RequestURI
		requestURI = addressFactory.createSipURI(toUsername, toAddress);
		requestURI.setTransportParam("udp");

		// Setting ContactHeader
		contactURI = addressFactory.createSipURI(getUsername(), getHost());
		contactURI.setPort(getPort());
		Address contactAddress = addressFactory.createAddress(contactURI);
		contactAddress.setDisplayName(getUsername());
		ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
		req.addHeader(contactHeader);

		// Setting viaHeaders
		viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = headerFactory.createViaHeader(getHost(), getPort(), "udp", "branch1");
		viaHeaders.add(viaHeader);
		req.addHeader(viaHeader);
		callIdHeader = sipProvider.getNewCallId();
		req.addHeader(callIdHeader);
		cSeqHeader = headerFactory.createCSeqHeader(1l, requestType);
		req.addHeader(cSeqHeader);
		maxForwards = headerFactory.createMaxForwardsHeader(70);
		req.addHeader(maxForwards);
		contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
		req.addHeader(contentTypeHeader);

	}

	/**
	 * Creates a ContactHeader based on the contact address
	 * 
	 * @throws ParseException
	 */
	public ContactHeader getContactHeader() throws ParseException {
		SipURI contactURI;
		contactURI = addressFactory.createSipURI(getUsername(), getHost());
		contactURI.setPort(getPort());
		Address contactAddress = addressFactory.createAddress(contactURI);
		contactAddress.setDisplayName(getUsername());
		ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
		return contactHeader;
	}

	/** This method is called by the SIP stack when a response arrives. */
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.SipListener#processResponse(javax.sip.ResponseEvent)
	 */
	public void processResponse(ResponseEvent evt) {
		LOGGER.debug("processResponse(" + evt.toString() + " )");
		Response response = evt.getResponse();
		int status = response.getStatusCode();

		// Handlen der verschiedenen Responses
		if (status == Response.TRYING) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processTrying();
		}
		if (status >= Response.OK && status <= 300) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processInfo("--Sent");
		} else {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processError("Previous message not sent: " + status);
		}
		if (status == Response.OK) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processOK(evt);
		}
		if (status == Response.RINGING) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processRinging();
		}
	}

	/**
	 * This method is called by the SIP stack when a new request arrives.
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sip.SipListener#processRequest(javax.sip.RequestEvent)
	 */
	public void processRequest(RequestEvent evt) {
		LOGGER.debug("processRequest(" + evt.toString() + " )");
		Request req = evt.getRequest();

		String method = req.getMethod();

		// Handlen der verschiedenen Requests
		if (method.equals("MESSAGE")) {
			FromHeader from = (FromHeader) req.getHeader("From");
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processMessage(from.getAddress().toString(), new String(req.getRawContent()));
			Response response = null;
			try { // Reply with OK
				response = messageFactory.createResponse(200, req);
				ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
				toHeader.setTag("888"); // This is mandatory as per the spec.
				ServerTransaction st = sipProvider.getNewServerTransaction(req);
				st.sendResponse(response);
			} catch (Throwable e) {
				e.printStackTrace();
				for (MessageProcessor messageProcessor : messageProcessors)
					messageProcessor.processError("Can't send OK reply.");
			}
		} else if (method.equals(Request.ACK)) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processAck(evt);
		} else if (method.equals(Request.INVITE)) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processInvite(evt);
		} else if (method.equals(Request.BYE)) {
			for (MessageProcessor messageProcessor : messageProcessors)
				messageProcessor.processBye(evt);
		}
	}

	/**
	 * This method is called by the SIP stack when there's no answer to a
	 * message. Note that this is treated differently from an error message.
	 */
	public void processTimeout(TimeoutEvent evt) {
		for (MessageProcessor messageProcessor : messageProcessors)
			messageProcessor.processError("Previous message not sent: " + "timeout");
	}

	/**
	 * This method is called by the SIP stack when there's an asynchronous
	 * message transmission error.
	 */
	public void processIOException(IOExceptionEvent evt) {
		for (MessageProcessor messageProcessor : messageProcessors)
			messageProcessor.processError("Previous message not sent: " + "I/O Exception");
	}

	/**
	 * This method is called by the SIP stack when a dialog (session) ends.
	 */
	public void processDialogTerminated(DialogTerminatedEvent evt) {
		for (MessageProcessor messageProcessor : messageProcessors)
			messageProcessor.processDialogTerminated(evt);
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

	public List<MessageProcessor> getMessageProcessors() {
		return messageProcessors;
	}

	public void addMessageProcessor(MessageProcessor newMessageProcessor) {
		messageProcessors.add(newMessageProcessor);
	}

	/**
	 * Registers the Server with the SIP Proxy
	 * 
	 * @param proxyAddress
	 * @return callId
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public String register(String proxyAddress) throws ParseException, InvalidArgumentException,
			SipException {
		// Create Register request with proxy as target
		SipURI from = addressFactory.createSipURI(getUsername(), getHost() + ":" + getPort());
		Address fromNameAddress = addressFactory.createAddress(from);
		fromNameAddress.setDisplayName(getUsername());
		FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, null);
		ToHeader toHeader = headerFactory.createToHeader(fromNameAddress, null);
		SipURI requestURI = addressFactory.createSipURI(toUsername, proxyAddress);
		requestURI.setTransportParam("udp");
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = headerFactory.createViaHeader(getHost(), getPort(), "udp", null);
		viaHeaders.add(viaHeader);
		CallIdHeader callIdHeader = sipProvider.getNewCallId();
		long sequenceNumber = 1;
		CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequenceNumber, Request.REGISTER);
		MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
		Request request = messageFactory.createRequest(requestURI, Request.REGISTER, callIdHeader, cSeqHeader,
				fromHeader, toHeader, viaHeaders, maxForwards);
		SipURI contactURI = addressFactory.createSipURI(getUsername(), getHost());
		contactURI.setPort(getPort());
		Address contactAddress = addressFactory.createAddress(contactURI);
		contactAddress.setDisplayName(getUsername());
		ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
		request.addHeader(contactHeader);
		// Send Register request to proxy
		sipProvider.sendRequest(request);
		return callIdHeader.getCallId();
	}

	/**
	 * Disconnects from a server using the one and only dialog trackt in the
	 * client!
	 * 
	 * @param serverDialog
	 */
	public void hangUp(Dialog serverDialog) {
		Request bye;
		try {
			bye = serverDialog.createRequest(Request.BYE);
			serverDialog.sendRequest(this.sipProvider.getNewClientTransaction(bye));
		} catch (TransactionDoesNotExistException e) {
			e.printStackTrace();
		} catch (TransactionUnavailableException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		}
	}

	public Response createResponse(int statusCode, Request request) throws ParseException {
		return messageFactory.createResponse(statusCode, request);
	}

	public void sendResponse(Response response) throws SipException {
		sipProvider.sendResponse(response);
	}

	public ServerTransaction getNewServerTransaction(Request request) throws TransactionAlreadyExistsException, TransactionUnavailableException {
		return sipProvider.getNewServerTransaction(request);		
	}

}
