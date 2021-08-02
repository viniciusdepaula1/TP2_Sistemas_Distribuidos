package pub;

import java.util.Set;
import java.util.SortedSet;
import java.util.Iterator;

import core.Message;
import core.MessageImpl;
import core.PubSubCommand;

public class NotifyCommand implements PubSubCommand{

	@Override
	public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers) {		//atualiza meu log
		
		Message response = new MessageImpl();
		
		response.setContent("Message notified: " + m.getContent());
		
		response.setType("notify_ack");

		String messageContent[] = m.getContent().split(" ");

		synchronized (log){					
			log.add(m);

			if(messageContent[0].length() == 6){	//if Unlock
				log.notify();			
			}
		}

		//System.out.println("Number of Log itens of an Observer " + m.getBrokerId() + " : " + log.size());

		Iterator<Message> it = log.iterator();
		//System.out.println("logs até o momento");
		while(it.hasNext()){
			Message aux = it.next();
			//System.out.print(aux.getLogId() + " " + aux.getContent() + " | ");
		}
		//System.out.println(" ");


		return response;

	}

}

