package com.seafile.sharing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
//发出同步信息
public class requestSyncFunction extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
    public Map<String,ArrayList<String[]>> map=new LinkedHashMap<String,ArrayList<String[]>>();
    public int type;
    public String infos=new String();
   
    public requestSyncFunction(int t,Map<String,ArrayList<String[]>> m,String infos)
    {
    	this.type=t;
    	this.map=m;
    	this.infos=infos;
    	 
    }
    
    
    
	public synchronized  void init()
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
	public  void run()
    {
		init();
		SynPacket synpa=new SynPacket(type,map,infos);
		byte[] messages=new byte[1024*1024];
		  
	    try {
			  ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
			  ObjectOutputStream oos = new ObjectOutputStream(baos);
			  oos.writeObject(synpa);
			  messages=baos.toByteArray();
			  packet=new DatagramPacket(messages,messages.length,addr,FileSharing.sync_port);
			  socket.send(packet);
			  System.out.println("已经发送settle表");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		  
	    
	     
	  }

}
