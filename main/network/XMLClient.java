package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import logs.Logger;
import logs.Logging;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLClient{
	
	private int port;
	private String host;
	private DefaultHandler handler;

	private PrintWriter writer;
	private Socket socket;
	public boolean connected = false;
	
	public String lastMessage;
	
	private ClientListener listener;
	
	Logger logger;
	
	public XMLClient(String host, int port,DefaultHandler handler,ClientListener listener){
		this.host = host;
		this.port = port;
		this.handler = handler;
		this.listener = listener;
		logger = Logging.getLogger(XMLClient.class.getName());
		logger.setCategory("internet");
		run();
	}

	private void run() {
		try {
			configureCommunication();
			startReceiverThread();
			connected = true;
		} catch (IOException e) {
			logger.info("Nie mozna znalezc serwera. Sprawdz połączenie z internetem.");
		}
		
	}

	private void startReceiverThread() {
		Thread receiver = new Thread(new MessageReceiver());
		receiver.start();
	}

	private void configureCommunication() throws IOException,UnknownHostException {
		socket = new Socket(host, port);
		
		writer = new PrintWriter(socket.getOutputStream());
		
		logger.info("Po\u0142\u0105czono z serwerem.");
		
	}
	
	public boolean isConnected(){
		return connected;
	}
	
	public void tryConnecting(){
		if (!connected){
			run();
		}
	}
	
	public void sendMessage(String message){

		if (connected){
		
			writer.println(message);
			writer.flush();
			
		}
		
	}
	
	public void closeConnection(){
		logger.fine("Wywolana zostala funkcja XMLClient.closeConnection()");
		if (connected){
			writer.close();
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		logger.info("Zakonczylismy połączenie");
	}
	
	public class MessageReceiver implements Runnable {

		@Override
		public void run() {
			
			try {
			
				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser saxParser = factory.newSAXParser();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				
				for (String line; (line = reader.readLine()) != null;) {
					logger.fine("Dostalismy : "+line);
					if (!line.equalsIgnoreCase("<?xml version=\"1.0\"?>")
							&& !line.equalsIgnoreCase("<?xml version='1.0' ?>")){
						if (line.endsWith("<?xml version=\"1.0\"?>")){
							line = line.replace("<?xml version=\"1.0\"?>", "");
						}
						logger.fine(" //przetwarzamy");
						lastMessage = line;
						saxParser.parse(new InputSource(new StringReader(line)), handler);
					} else logger.fine("");
					logger.fine("Przetworzylismy wiadomosc, czekamy na następną");
				}

			
			} catch (IOException e){
				logger.info(new Date()+" IOException w sockecie");
				listener.connectionClosed();
			} catch (SAXException e){
				logger.error(new Date()+" SAXException w sockecie");
				listener.connectionClosed();
			} catch (Exception e){
				logger.error(new Date()+" Inny wyjatek w sockecie");
				logger.error(e.getStackTrace().toString());
				listener.connectionClosed();
			}
			logger.info(new Date()+" Koniec przetwarzania w sockecie");
		}
		
	}
	
}
