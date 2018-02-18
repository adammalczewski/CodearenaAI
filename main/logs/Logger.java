package logs;

import java.util.LinkedList;

import logs.LogMessage.LogLevel;

public class Logger {

	private String className;
	private String category = null;
	
	private boolean recording = false;
	
	private boolean enabled = true;
	
	private LinkedList<LogMessage> messagesList = new LinkedList<LogMessage>();
	
	Logger(String className){
		this.className = className;
	}
	
	Logger(String className, String category){
		this.className = className;
		this.category = category;
	}
	
	public String getClassName(){
		return className;
	}
	
	public void setCategory(String category){
		this.category = category;
	}
	
	private void logMessage(String message,String category, LogLevel lvl){
		
		if (true) return;
		
		LogMessage msg = new LogMessage();
		
		msg.category = category;
		msg.msg = message;
		msg.level = lvl;
		
		if (Logging.addMessage(msg) && recording) messagesList.add(msg);
		
		
	}
	
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}
	
	private void logMessage(String message,LogLevel lvl){
		
		if (true) return;
		
		LogMessage msg = new LogMessage();
		
		if (category == null){
			msg.category = null;
			Logging.loggingLogger.warning("Nie podano kategorii w wiadomosci do logu.");
		} else msg.category = category;
		
		msg.msg = message;
		msg.level = lvl;
		
		if (recording) messagesList.add(msg);
		Logging.addMessage(msg);
		
	}
	
	public void severe(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.SEVERE);
		
	}
	
	public void severe(String message){
		
		if (enabled) logMessage(message,LogLevel.SEVERE);
		
	}
	
	public void severe(){
		
		if (enabled) severe("");
		
	}
	
	public void error(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.ERROR);
		
	}
	
	public void error(String message){
		
		if (enabled) logMessage(message,LogLevel.ERROR);
		
	}
	
	public void error(){
		
		if (enabled) error("");
		
	}
	
	public void warning(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.WARNING);
		
	}
	
	public void warning(String message){
		
		if (enabled) logMessage(message,LogLevel.WARNING);
		
	}
	
	public void warning(){
		
		if (enabled) warning("");
		
	}
	
	public void info(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.INFO);
		
	}
	
	public void info(String message){
		
		if (enabled) logMessage(message,LogLevel.INFO);
		
	}
	
	public void info(){
		
		if (enabled) info("");
		
	}
	
	public void fine(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.FINE);
		
	}
	
	public void fine(String message){
		
		if (enabled) logMessage(message,LogLevel.FINE);
		
	}
	
	public void fine(){
		
		if (enabled) fine("");
		
	}
	
	public void finer(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.FINER);
		
	}
	
	public void finer(String message){
		
		if (enabled) logMessage(message,LogLevel.FINER);
		
	}
	
	public void finer(){
		
		if (enabled) finer("");
		
	}
	
	public void finest(String message,String category){
		
		if (enabled) logMessage(message, category, LogLevel.FINEST);
		
	}
	
	public void finest(String message){
		
		if (enabled) logMessage(message,LogLevel.FINEST);
		
	}
	
	public void finest(){
		
		if (enabled) finest("");
		
	}
	
	public void startRecording(){
		recording = true;
	}
	
	public LinkedList<LogMessage> stopRecording(){
		
		LinkedList<LogMessage> result = messagesList;
		messagesList = new LinkedList<LogMessage>();
		recording = false;
		
		return result;
		
	}
	
}
