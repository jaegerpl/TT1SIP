package igmp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import org.apache.log4j.Logger;

import textclient.TextClient;

public class IGMPListener extends IGMPComponent{
	
	private TextClient gui; // The gui where the received messages are displayed

	// Name des Loggers
	public static final String TAG = "IGMPListener";

	// Loggerinstanz
	private static final Logger LOGGER = Logger.getLogger(IGMPListener.class);

	/**
	 * Initialisiert den MulticastSocket 
	 * 
	 * @param ip			IPAdresse der Multicastgruppe
	 * @param port			Port auf den 
	 * @throws IOException  Fehler beim erzeugen des IPAdressen-Objekts oder Port
	 */
	public void initialize(InetAddress ip, int port, TextClient gui) throws IOException{

		this.gui = gui;
		// Socket anlegen und Gruppe joinen
		mSocket = new MulticastSocket(port);
		mcastAdr = ip;

		mSocket.joinGroup(mcastAdr);

		// IP und Port für später speichern
		mcastAdr = ip;
		this.port = port;
		pack = new DatagramPacket(buf, buf.length);

	}

	@Override
	public void run() {

		LOGGER.debug("run()-Methode aufgerufen");
		LOGGER.debug("isRunning: "+isRunning);

		while(isRunning) {

			LOGGER.debug("Schleife erreicht");

			try {

				// Auf Daten warten
				mSocket.receive(pack);

				//Empfangene Daten ausgeben
				byte[] text = pack.getData();
				String msg = new String(text);
				gui.processMessage("MulticastGruppe", msg.trim());
			} catch (IOException e) {

				LOGGER.error("Fehler beim Lesen aus dem MulticastSocket: "+e);

				stop();

			}


		}

		LOGGER.debug("Schleife beendet");

		try {

			mSocket.leaveGroup(mcastAdr);

		} catch (IOException e) {
			LOGGER.error("Fehler beim verlassend er Multicastgruppe: "+e);
		} finally {
			mSocket.close();
		}

		LOGGER.debug("MulticastSocket abgebaut");

	}

}