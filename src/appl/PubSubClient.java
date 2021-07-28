package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import core.Message;
import core.MessageImpl;
import core.Server;
import core.client.Client;
import java.util.Set;

public class PubSubClient {				//recebe e publica
	
	private Server observer;
	private ThreadWrapper clientThread;
	
	private String clientAddress;
	private int clientPort;
	
	public PubSubClient(){
		//this constructor must be called only when the method
		//startConsole is used
		//otherwise the other constructor must be called
	}
	
	public PubSubClient(String clientAddress, int clientPort){
		this.clientAddress = clientAddress;
		this.clientPort = clientPort;
		observer = new Server(clientPort);
		clientThread = new ThreadWrapper(observer);
		clientThread.start();
	}
	
	public void subscribe(String brokerAddress, int brokerPort){
		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("sub");
		msgBroker.setContent(clientAddress+":"+clientPort);
		Client subscriber = new Client(brokerAddress, brokerPort);
		System.out.println(subscriber.sendReceive(msgBroker).getContent());
	}
	
	public void unsubscribe(String brokerAddress, int brokerPort){
		
		Message msgBroker = new MessageImpl();
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setType("unsub");
		msgBroker.setContent(clientAddress+":"+clientPort);
		Client subscriber = new Client(brokerAddress, brokerPort);
		subscriber.sendReceive(msgBroker);
	}

	public void consume(String resource) throws InterruptedException {
		System.out.println("usando recurso = " + resource);
		Thread.sleep(30000);
		System.out.println("cabei");
	}

	public String verificaVez(int myLogId, String myVar) throws InterruptedException{
		Set<Message> logs = this.observer.getLogMessages();

		synchronized (logs) {
			while(verifyLogs(logs, myLogId, myVar) == false) {
				System.out.println("esperando");
				logs.wait();
			}
		}

		return myVar;
	}
	
	public void unlock(String message, String brokerAddress, int brokerPort){
		Message msgUnlock = new MessageImpl();
		msgUnlock.setBrokerId(brokerPort);
		msgUnlock.setType("unlock");
		msgUnlock.setContent("Unlock " + message);

		Client publisher = new Client(brokerAddress, brokerPort);			//outro cara ta esperando
		Message received = publisher.sendReceive(msgUnlock);

		System.out.println(received.getLogId());			//id na minha maquina
		System.out.println(received.getType());
		System.out.println(received.getContent());
	}

	public boolean verifyLogs(Set<Message> logs, int myLogId, String myVar) {		//se tiver mais locks que unlocks eu olho a minha position
		System.out.println("entrei verify logs");																	//se 3locks a mais eu tenho q esperar aparecer 3 unlocks na minha frente
		Iterator<Message> it = logs.iterator();
		Integer locks = 0;																												//antes de mim tem 2 lock? tenho q esperar 2 unlocks
		Integer unlocks = 0;

		while(it.hasNext()){
			Message aux = it.next();
			int id = aux.getLogId();
			String content = aux.getContent();

			if(id != myLogId){
				System.out.println("dif");																	//se 3locks a mais eu tenho q esperar aparecer 3 unlocks na minha frente

				String[] divideMessage = content.split(" ");
				
				if(divideMessage.length == 2) {
					System.out.println("mensagem: " + divideMessage[0] + " " + divideMessage[1]);
					if(divideMessage[0].equals("Lock") && divideMessage[1].equals(myVar) && id < myLogId){
						locks += 1;
					}
					if(divideMessage[0].equals("Unlock") && divideMessage[1].equals(myVar)){
						unlocks += 1;
					}
				}
			} 
		}

		System.out.println("numero de locs: " + locks + " numero de unlocks: " + unlocks);

		if(locks == unlocks)
			return true;
		
		return false;
	}

	public String publish(String message, String brokerAddress, int brokerPort) throws InterruptedException {
		Message msgPub = new MessageImpl();
		msgPub.setBrokerId(brokerPort);
		msgPub.setType("pub");
		msgPub.setContent("Lock " + message);
		
		Client publisher = new Client(brokerAddress, brokerPort);			//outro cara ta esperando
		Message received = publisher.sendReceive(msgPub);			//não saio de 61 até eu receber a resposta do broker quando vai ser o receive? pubcommand

		System.out.println(received.getLogId());			//id na minha maquina
		System.out.println(received.getType());
		System.out.println(received.getContent());
		//accquire sucesso? oq q eu faço?????
		//Set<Message> logs = this.observer.getLogMessages();

		//aguardo verificando
		return verificaVez(received.getLogId(), message);
		//System.out.println("Resultado: = " + value);
		
		//vai pro próximo
		//os dois vão tentar utilizar a var
		//aqui vamos entrar em acordo
		//um acordo entre pubsubclients

		//o accquire do marcos chegou primeiro em todos os logs dos pubsubclients.
		//basta olhar o log ver quem chegou primeiro
		//como vou esperar?  -- mandar pro Brocker q eu terminei de usar??? o accquire precisa do release
	}
	
	public Set<Message> getLogMessages(){
		return observer.getLogMessages();
	}

	public void stopPubSubClient(){
		System.out.println("Client stopped...");
		observer.stop();
		clientThread.interrupt();
	}
		
	public void startConsole(){
		Scanner reader = new Scanner(System.in);  // Reading from System.in
		System.out.print("Enter the client address (ex. localhost): ");
		String clientAddress = reader.next();
		System.out.print("Enter the client port (ex.8080): ");
		int clientPort = reader.nextInt();
		System.out.println("Now you need to inform the broker credentials...");
		System.out.print("Enter the broker address (ex. localhost): ");
		String brokerAddress = reader.next();
		System.out.print("Enter the broker port (ex.8080): ");
		int brokerPort = reader.nextInt();
		
		observer = new Server(clientPort);
		clientThread = new ThreadWrapper(observer);
		clientThread.start();
		
		Message msgBroker = new MessageImpl();
		msgBroker.setType("sub");
		msgBroker.setBrokerId(brokerPort);
		msgBroker.setContent(clientAddress+":"+clientPort);
		Client subscriber = new Client(brokerAddress, brokerPort);
		subscriber.sendReceive(msgBroker);
		
		System.out.println("Do you want to subscribe for more brokers? (Y|N)");
		String resp = reader.next();
		
		if(resp.equals("Y")||resp.equals("y")){
			String message = "";
			Message msgSub = new MessageImpl();
			msgSub.setType("sub");
			msgSub.setContent(clientAddress+":"+clientPort);
			while(!message.equals("exit")){
				System.out.println("You must inform the broker credentials...");
				System.out.print("Enter the broker address (ex. localhost): ");
				brokerAddress = reader.next();
				System.out.print("Enter the broker port (ex.8080): ");
				brokerPort = reader.nextInt();
				subscriber = new Client(brokerAddress, brokerPort);
				msgSub.setBrokerId(brokerPort);
				subscriber.sendReceive(msgSub);
				System.out.println(" Write exit to finish...");
				message = reader.next();
			}
		}
		
		System.out.println("Do you want to publish messages? (Y|N)");
		resp = reader.next();
		if(resp.equals("Y")||resp.equals("y")){
			String message = "";			
			Message msgPub = new MessageImpl();
			msgPub.setType("pub");
			while(!message.equals("exit")){
				System.out.println("Enter a message (exit to finish submissions): ");
				message = reader.next();
				msgPub.setContent(message);
				
				System.out.println("You must inform the broker credentials...");
				System.out.print("Enter the broker address (ex. localhost): ");
				brokerAddress = reader.next();
				System.out.print("Enter the broker port (ex.8080): ");
				brokerPort = reader.nextInt();
				
				msgPub.setBrokerId(brokerPort);
				Client publisher = new Client(brokerAddress, brokerPort);
				publisher.sendReceive(msgPub);
				
				Set<Message> log = observer.getLogMessages();
				
				Iterator<Message> it = log.iterator();
				System.out.print("Log itens: ");
				while(it.hasNext()){
					Message aux = it.next();
					System.out.print(aux.getContent() + aux.getLogId() + " | ");
				}
				System.out.println();

			}
		}
		
		System.out.print("Shutdown the client (Y|N)?: ");
		resp = reader.next(); 
		if (resp.equals("Y") || resp.equals("y")){
			System.out.println("Client stopped...");
			observer.stop();
			clientThread.interrupt();
			
		}
		
		//once finished
		reader.close();
	}
	
	class ThreadWrapper extends Thread{
		Server s;
		public ThreadWrapper(Server s){
			this.s = s;
		}
		public void run(){
			s.begin();
		}
	}	

}
