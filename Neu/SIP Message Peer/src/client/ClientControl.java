/**
 * 
 */
package client;

import java.util.ArrayList;
import java.util.Map.Entry;

import javax.sip.Dialog;
import javax.swing.table.AbstractTableModel;

/**
 * Handles the SIP stuff
 * - establishing new session
 * - quitting session
 * - providing data for the gui
 * 
 * @author Pascal
 *
 */
public class ClientControl extends AbstractTableModel{

	// servers that have been send an INVITE
	private ArrayList<Pair> knownServers = new ArrayList<Pair>();

	// servers that are successfully connected to the client (INVITE, OK and ACK are transmitted)
	private ArrayList<Pair> connectedServers = new ArrayList<Pair>();

	// servers that have been sent an BYE, but did not reply with an OK
	private ArrayList<Pair> disconnectingServers = new ArrayList<Pair>();

	// list of connected servers for the GUI
	private ArrayList<String> serverTable = new ArrayList<String>();

	private UserAgentClient client;

	public ClientControl(UserAgentClient client){
		this.client = client;
//		this.addTableModelListener(client);
	}

	/**
	 * Adds a server to the list of knownServers.
	 * 
	 * @param dia
	 * @param address
	 */
	public void addServer(Dialog dia, String address){
		knownServers.add(new Pair(dia, address));
	}

	/**
	 * Moves the server from the list of knownServers to the connectedServers List
	 * and also adds it to the serverTable to display in the gui.
	 * 
	 * @param dia
	 */
	public void markServerAsConnected(Dialog dia){
		// remove in knownServers list
		String diaID = dia.getDialogId();
		String address;
		boolean knownServerNowConnected = false;

		for(Pair p : knownServers){
			if(diaID.equals(p.getKey().getDialogId())){
				address = p.getValue();
				knownServers.remove(p.getKey());

				// add to connectedServers list
				connectedServers.add(new Pair(dia, address));

				// add to serverTable for GUI
				serverTable.add(address);
				fireTableChanged(null);

				knownServerNowConnected = true;

				break;
			}
		}

		if(knownServerNowConnected == false){
			//TODO send DECLINE
		}
	}

	/**
	 * Remove server from all lists and in the GUI
	 * 
	 * @param dia
	 */
	public void removeServer(Dialog dia){
		String diaID = dia.getDialogId();
		String address;

		for(Pair p : connectedServers){
			if(diaID.equals(p.getKey().getDialogId())){
				address = p.getValue();
				knownServers.remove(p.getKey());

				serverTable.remove(serverTable.indexOf(address));
				fireTableChanged(null);
			}
		}
	}

	public void disconnectServer(Dialog dia) {

	}

	/**
	 * Returns the dialog to the given address string or null if nothing could be found.
	 * 
	 * @param address
	 * @return
	 */
	public Dialog getServerDialog(String address) {
		for(Pair p : connectedServers){
			if(p.getValue().equals(address)){
				return p.getKey();
			}
		}
		return null;
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public int getRowCount() {
		return serverTable.size();
	}

	@Override
	public Object getValueAt(int row, int coulmn) {
		return serverTable.get(row);
	}
	
	public void setValueAt(Object value, int row, int column) {
		serverTable.add(row, (String) value);
	}
	
	public String getColumnName(int i) {
		return "Servers";
	}

	class Pair implements Entry<Dialog, String> {

    	private String adress;
    	private Dialog dialog; 
    	
    	public Pair(Dialog dai, String serveraddress){
    		this.dialog = dai;
    		this.adress = serveraddress;
    	}
		@Override
		public Dialog getKey() {
			return dialog;
		}

		@Override
		public String getValue() {
			return adress;
		}

		@Override
		public String setValue(String server) {
			String oldserver = this.adress;
			this.adress = server;
			return oldserver;
		}

		public String toString(){
			return adress;
		}
    }

}