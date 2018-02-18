package logs;

import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Logging {

	static private LinkedList<Logger> loggerList = new LinkedList<Logger>();
	
	//ConcurrentLinkedQueue.size() ma zlozonosc liniowa
	static private ConcurrentLinkedQueue<LogMessage> log = new ConcurrentLinkedQueue<LogMessage>();
	
	static public Logger loggingLogger;
	
	static private boolean enabled = true;
	
	static public boolean addMessage(LogMessage msg){
		
		if (enabled){
		
			//if (msg.level == LogLevel.FINE || msg.level == LogLevel.FINEST) return;
			
			Thread currentThread = Thread.currentThread();
			if (currentThread.getName() != null){
				msg.threadName = currentThread.getName();
			}
			
			msg.time = new Date();
			
			log.add(msg);
			
			return true;
		
		} else return false;
		
	}
	
	static public void setEnabled(boolean enabled){
		Logging.enabled = enabled;
	}
	
	static public boolean isEmpty(){
		
		return log.isEmpty();
		
	}
	
	static public LogMessage getMessage(){
		
		try {
			return log.remove();
		} catch (NoSuchElementException e){
			return null;
		}
		
	}
	
	static public Logger getLogger(String className){
		
		if (loggingLogger == null){
			loggingLogger = new Logger("Logger");
			loggingLogger.setCategory("logging");
		}
		
		Logger newLogger = new Logger(className);
		
		loggerList.add(newLogger);
		
		return newLogger;
		
	}
	
	static public Logger getLogger(String className, String category){
		
		Logger newLogger = getLogger(className);
		newLogger.setCategory(category);
		
		return newLogger;
		
	}
	
	static public LogDisplayer complexLogDisplayer(){
		return (LogMessage msg) -> msg.threadName + " - "+msg.level + " ["+msg.category+"] : "+msg.msg;
	}
	
	static public LogDisplayer complexTimeLogDisplayer(){
		return (LogMessage msg) -> msg.time.toString() + " " + msg.threadName + " - "+msg.level + " ["+msg.category+"] : "+msg.msg;
	}
	
	static public LogDisplayer simpleLogDisplayer(){
		return (LogMessage msg) -> msg.level + " : "+msg.msg;
	}
	
	static public LogDisplayer simpleTimeLogDisplayer(){
		return (LogMessage msg) -> msg.time + " " + msg.level + " : "+msg.msg;
	}
	
	static public LogDisplayer textLogDisplayer(){
		return (LogMessage msg) -> msg.msg;
	}
	
	static public LogDisplayer textTimeLogDisplayer(){
		return (LogMessage msg) -> msg.time + " : " + msg.msg;
	}
	
}
