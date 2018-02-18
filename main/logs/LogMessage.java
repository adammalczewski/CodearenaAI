package logs;

import java.util.Date;


public class LogMessage {
	
	public final static String CATEGORY_STACK_TRACE = "Stack Trace";

	public enum LogLevel{
		FATAL, SEVERE, ERROR, WARNING, INFO, FINE, FINER, FINEST
	}
	
	public LogMessage(){
		
	}
	
	public LogMessage(String msg,String category,LogLevel lvl){
		this.msg = msg;
		this.category = category;
		this.level = lvl;
	}
	
	public LogLevel level;
	public String threadName;
	public String category; //specjalna kategoria - "Stack Trace", wtedy msg = "NAZWA_FUNKCJI" lub "~NAZWA_FUNKCJI"
	public String msg;
	public Date time;
	
}
