package main;

public class DSEntry {
	
	private String file;
	private String ip;
	private int port;
	public DSEntry(String file, String ip, int port) {
		super();
		this.file = file;
		this.ip = ip;
		this.port = port;
	}
	
	public String getFile() {
		return file;
	}
	
	public String getEndpoint() {
		return ip +" "+port;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	
	public String toString() {
		return file+" "+ip+" "+port+";";
	}

}
