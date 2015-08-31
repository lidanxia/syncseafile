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
	         messages = baos.toByteArray();   //Packet类中除去data,其它字段的大小是261左右个字节，实验得出的。
	         baos.close();  
	         oos.close(); 
	    	
	         packet = new DatagramPacket(messages, messages.length,addr, port); 
	         socket.send(packet);
	         if(pt.islast==true)
	           System.out.println("@@@@@@@@ 发送本节点最后一个文件的最后一个包 ---pt.islast:  "+pt.islast);
	         
	         System.out.println("发送ING-- "+file_name+"-- "+sub_fileID+"---packet:"+i);
	         if(pt.islast==true)//判断每一个包，如果它的islast标志是true，说明是我们要找的最后一个文件的最后一个包
	         {
	        	 System.out.println("@@@@@@@@@ 发完最后一个文件的最后一个包，开始发送 nextIP+end");
	        	 //知道发送方自己的IP，从savedMap中找到这个IP对应的下一个IP，下一个IP就是下一个要发送的节点
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
	     	 			if(it.hasNext())//如果有下一个，发送end+nextip,让下一个IP接着发送
	     	 			{
	     	 				nextip=(String)it.next();
	     	 				System.out.println("@@@@ 发送完最后一个文件后，发送nextIP:  "+nextip);
	     	 				String infos=nextip;
	     		        	requestSyncFunction rsf=new  requestSyncFunction(3,null,infos);
	     		        	rsf.start();
	     	 				break;
	     	 			}
	     	 			else//如果找不到下一个说明这是savedMap中最后一个发送文件的，发完同步就结束了
	     	 			{
	     	 				//将同步标志改回来，删除sync表
	     	 				System.out.println("本次同步结束了");
	     	 			}
	     	 			
	     	 		 }
	     	 	 }
	     	 	 //一个节点发完自己该发的最后一个文件后，且找到了下一个，对于这个结点来说，savedmap的使命就完成了，这时应该清空
	     	 	 //要不就和下一次同步的内容混了，
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
			 if(too<=plist[0].data_blocks+plist[0].coding_blocks) //直接发送number个包
			 {
				 for(int i=0;i<number;i++)
				    sendPacket(plist[from+i] ,plist[from+i].sub_fileID,plist[from+i].seqno,plist[from+i].filename);
			 }
			 else
			 {
				
				   for(int j=from;j<plist[0].data_blocks+plist[0].coding_blocks;j++) //不够的话，再从0开始发
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
