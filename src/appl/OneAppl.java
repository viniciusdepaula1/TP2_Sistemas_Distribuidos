package appl;

import java.util.Iterator;
import java.util.List;

import core.Message;

public class OneAppl {

	private static String serverIp;
	private static int serverPort;

	private static String clientIp;
	private static int clientPort;

	public static void setServerIp(String ip) { serverIp = ip; }
	public static String getServerIp() { return serverIp; }
	public static void setServerPort(int port) { serverPort = port; }
	public static int getServerPort() { return serverPort; }

	public static void setClientIp(String ip) { clientIp = ip; }
	public static String getClientIp() { return clientIp; }
	public static void setClientPort(int port) { clientPort = port; }
	public static int getClientPort() { return clientPort; } 

	public static void main(String[] args) throws InterruptedException {
		setServerIp(args[0]);
		setServerPort(Integer.parseInt(args[1]));
		setClientIp(args[2]);
		setClientPort(Integer.parseInt(args[3]));
		
		new OneAppl(true);
	}
	
	public OneAppl(){
		PubSubClient client = new PubSubClient();
		client.startConsole();
	}
	
	public OneAppl(boolean flag){
		PubSubClient client = new PubSubClient(clientIp, clientPort);
		String resources[] = {"X", "Y", "Z"};

		client.subscribe(serverIp, serverPort);
		
		try {
			String result = client.publish(resources[1], serverIp, serverPort);
			client.consume(result);
			client.unlock(resources[1], serverIp, serverPort);
			client.unsubscribe(serverIp, serverPort);
			client.stopPubSubClient();
		} catch (InterruptedException e) {
			System.out.println("thread error");
		}
	}

}
