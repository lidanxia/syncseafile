package com.seafile.sharing;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Iterator;

import com.seafile.seadroid2.BrowserActivity;

public class sendThread 
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	private String castIP="";
	private Packet []plist=null;
	private int port=0;
	private int from=0;
	private int number=0;
	private int mixfiles=0;
	public synchronized void inital(Packet []plist,String castIP,int port,int from,int number,int mixfiles)
	{
		this.plist=plist;
		this.castIP=castIP;
		this.port=port;
		this.from=from;
		this.number=number;
		this.mixfiles=mixfiles;
	}
	public synchronized  void init()
	{
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);	 
			addr=InetAddress.getByName(castIP);	
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	
	public synchronized void sendPacket(Packet pt,String sub_fileID,int i,String filename)
	{
		long start=System.currentTimeMillis();	
		byte[] messages=null;
		String file_name=null;
		if(filename!=null&&filename!="")
		   file_name=filename.substring(filename.lastIndexOf("/")+1);
		try {  
	         ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	         ObjectOutputStream oos = new ObjectOutputStream(baos);  
	         oos.writeObject(pt);
	         messages = baos.toByteArray();   //Packet���г�ȥdata,�����ֶεĴ�С��261���Ҹ��ֽڣ�ʵ��ó��ġ�
	         baos.close();  
	         oos.close(); 
	    	
	         packet = new DatagramPacket(messages, messages.length,addr, port); 
	         socket.send(packet);
	         if(pt.islast==true)
	           System.out.println("@@@@@@@@ ���ͱ��ڵ����һ���ļ������һ���� ---pt.islast:  "+pt.islast);
	         
	         System.out.println("����ING-- "+file_name+"-- "+sub_fileID+"---packet:"+i);
	         if(pt.islast==true)//�ж�ÿһ�������������islast��־��true��˵��������Ҫ�ҵ����һ���ļ������һ����
	         {
	        	 System.out.println("@@@@@@@@@ �������һ���ļ������һ��������ʼ���� nextIP+end");
	        	 //֪�����ͷ��Լ���IP����savedMap���ҵ����IP��Ӧ����һ��IP����һ��IP������һ��Ҫ���͵Ľڵ�
	        	 synchronized(FileSharing.savedMap)
	        	 {
	        	 String ip=BrowserActivity.getIp();
	        	 Iterator it = FileSharing.savedMap.keySet().iterator();
	        	 String nextip;
	     	 	 while (it.hasNext())
	     	 	 {
	     	 		 String key; 
	     	 		
	     	 		 key=(String)it.next();
	     	 		 if(ip.equals(key))
	     	 		 {
	     	 			if(it.hasNext())//�������һ��������end+nextip,����һ��IP���ŷ���
	     	 			{
	     	 				nextip=(String)it.next();
	     	 				System.out.println("@@@@ ���������һ���ļ��󣬷���nextIP:  "+nextip);
	     	 				String infos=nextip;
	     		        	requestSyncFunction rsf=new  requestSyncFunction(3,null,infos);
	     		        	rsf.start();
	     	 				break;
	     	 			}
	     	 			else//����Ҳ�����һ��˵������savedMap�����һ�������ļ��ģ�����ͬ���ͽ�����
	     	 			{
	     	 				//��ͬ����־�Ļ�����ɾ��sync��
	     	 				System.out.println("����ͬ��������");
	     	 			}
	     	 			
	     	 		 }
	     	 	 }
	     	 	 //һ���ڵ㷢���Լ��÷������һ���ļ������ҵ�����һ����������������˵��savedmap��ʹ��������ˣ���ʱӦ�����
	     	 	 //Ҫ���ͺ���һ��ͬ�������ݻ��ˣ�
	     	 	FileSharing.savedMap.clear();
	        	 
	         }
	         }
	       
	        }  
		    catch (SocketException e) 
	        {
		      return;
	        }
	        catch(Exception e) 
	        {   
	            e.printStackTrace();  
	        } 
		 FileSharing.total_sending_length+=messages.length;
		 long end=System.currentTimeMillis();
		 FileSharing.total_sending_timer+=(end-start);
		 
		 String []bb=sub_fileID.split("-");
		// FileSharing.writeLog("send-one-packet:"+bb[1]+"--"+bb[bb.length-1]+"---"+i+",	"+(end-start)+"ms,	"+"\r\n");
	}
		
    public synchronized void sending() 
	{ 
		long start=System.currentTimeMillis();
		 init();
		 if(plist!=null&&plist.length>0)
		 {  
			 if(mixfiles==1)
			 {
				 for(int i=0;i<number;i++)
					 if(plist[i]!=null)
					    sendPacket(plist[i] ,plist[i].sub_fileID,plist[i].seqno,plist[i].filename); 
			 }
			 else
			 {
			 int too=from+number;
			 if(too<=plist[0].data_blocks+plist[0].coding_blocks) //ֱ�ӷ���number����
			 {
				 for(int i=0;i<number;i++)
				    sendPacket(plist[from+i] ,plist[from+i].sub_fileID,plist[from+i].seqno,plist[from+i].filename);
			 }
			 else
			 {
				
				   for(int j=from;j<plist[0].data_blocks+plist[0].coding_blocks;j++) //�����Ļ����ٴ�0��ʼ��
					  sendPacket(plist[j] ,plist[j].sub_fileID,plist[j].seqno,plist[j].filename);
				   int off=from+number-(plist[0].data_blocks+plist[0].coding_blocks);
				   for(int i=0;i<off;i++)
					  sendPacket(plist[i] ,plist[i].sub_fileID,plist[i].seqno,plist[i].filename);
			}
			}
	    }	
		 long end=System.currentTimeMillis();
		 String []bb=plist[0].sub_fileID.split("-");
	//	 FileSharing.writeLog("send-one-block:"+bb[1]+"--"+bb[bb.length-1]+",	"+(end-start)+"ms,	"+"\r\n");
    }

	public void destroy() 
	{	
		if(socket!=null)
		  socket.close();
	
	}

}
