package com.seafile.sharing;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

public class SendServerTime extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public List<FileMtimeData> fileMtimes=null;
    public SendServerTime(List<FileMtimeData> fileMtimes)
	 {
		 this.fileMtimes=fileMtimes;
	 }
	public void init()
	{
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);	 
			addr=InetAddress.getByName(FileSharing.bcastaddress);	
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	public void run()
	{
		for(int i=0;i<fileMtimes.size();i++)
		{
		byte[]messages=null;
		try {  
	         ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	         ObjectOutputStream oos = new ObjectOutputStream(baos);  
	         oos.writeObject(fileMtimes.get(i));
	         messages = baos.toByteArray();   //Packet类中除去data,其它字段的大小是261左右个字节，实验得出的。
	         baos.close();  
	         oos.close(); 
	    	
	         packet = new DatagramPacket(messages, messages.length,addr, FileSharing.FileMtimePort); 
	         socket.send(packet);
	        }  
		    catch (SocketException e) 
	        {
		      return;
	        }
	        catch(Exception e) 
	        {   
	            e.printStackTrace();  
	        } 
		}
	}

}
