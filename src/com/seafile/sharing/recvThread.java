package com.seafile.sharing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Timer;

import com.seafile.seadroid2.data.DatabaseHelper;

import android.os.Message;


public class recvThread extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public boolean running=true;
	public byte[] encodedFrame=null;
	public ArrayList<String> filesID=new ArrayList<String>();
	public ArrayList<Packet[]> lossFiles=new ArrayList<Packet[]>();
	public ArrayList<int[]> pktIDs=new ArrayList<int[]>();  //���յ��Ķ�Ӧ�ļ����Ӱ������
	public Map<String,Timer> recvTimers=new HashMap<String,Timer>();
	private String localIP="";
	public int port=0;
	public Date nowDate=null;
	public Date curDate=null;
	public Timer recvtimer=null;
	public recvTask recvtask=null;
	public Packet []recvPacket=null;
	public String recvIP=null;
	public String mess=null;
	public String message=null;
	public final int fund_time=500; //�����೤ʱ��û�յ����ݰ��ͷ��ͷ�����
	recvThread(String localIP,int port)
	{
		
		this.localIP="/"+localIP;
		this.port=port;
	}
	public void init()
	{
		try {	
			socket= new DatagramSocket(null); 
			socket.setReuseAddress(true); 
			socket.bind(new InetSocketAddress(port)); 
			socket.setBroadcast(true);		
			encodedFrame=new byte[FileSharing.blocklength+1000];
		
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
		 
	}
	@Override
	public void run() 
	{
	 init();
	 while(running)	
	  {
	    packet = new DatagramPacket(encodedFrame, encodedFrame.length);
		try {
		socket.receive(packet);
	    } catch (SocketException e) 
	     {
		  break;
	     }
	   catch (IOException e) 
	    {	
         e.printStackTrace();
	     System.out.println(e.toString());
	    }
		recvIP=packet.getAddress().toString();
	//	 System.out.println("@@@@@  recvIP:  "+recvIP);
	//	 System.out.println("@@@@@  localIP:  "+localIP);
	  if (!recvIP.equals(localIP)&& packet.getData().length != 0) 
	     {
		   Packet pt=null;
		                //  ���ղ�Ϊ�գ��Ҳ����Լ��İ� 
		   try
		   {
			   ByteArrayInputStream ba=new ByteArrayInputStream(packet.getData());
			   ObjectInputStream oi=new ObjectInputStream(ba);
			   pt=(Packet)oi.readObject();
			   ba.close();
			   oi.close();
		   }catch(Exception e)
		   {
			   System.out.println(e.toString());
			    e.printStackTrace();
		   }
		   String file_name="";
		   if(pt.filename!=null&&pt.filename!="")
		     file_name=pt.filename.substring(pt.filename.lastIndexOf("/")+1);
		   String s="���յ��Ӱ���id�ţ�"+pt.sub_fileID+"---filename: "+file_name;  
		   System.out.println(s);
		  // FileSharing. writeLog(s+"\r\n");
		   if(pt.type==1||pt.type==2)
		   {
			   recvPacket=new Packet[1];
	           recvPacket[0]=pt;
	           System.out.println("�յ����Ƿ�����");
	           Message msg = new Message();
	           msg .obj = recvPacket;
	           msg .arg1=1;
	           FileSharing.myHandler.sendMessage(msg);	   	 
		   }
		   else
		   { 
			  String[] fileID_break=pt.sub_fileID.split("--");
			  if(!filesID.contains(pt.sub_fileID))
			  {
				 int offset=0;  //���ļ������б��еĵ�offset������
				 int pktLength=0; //���ܵİ������˵�pktLength����
				 if(recvTimers.containsKey(pt.sub_fileID))
				 {
					 recvTimers.get(pt.sub_fileID).cancel();
		    	     recvTimers.remove(pt.sub_fileID);    
				 }
				 synchronized(FileSharing.subfileTimers)
				 {
				   if(FileSharing.subfileTimers.containsKey(fileID_break[0]))
				    {
					 System.out.println("ȡ�����ʱ��");
				//	 FileSharing.writeLog("ȡ�����ʱ��"+"\r\n");
		             FileSharing.subfileTimers.get(fileID_break[0]).cancel();
		             FileSharing.subfileTimers.remove(fileID_break[0]);
				    }
				 }
					Random random = new Random();
					long delay=fund_time+random.nextInt(fund_time);
					long frequency=fund_time*5;
					//��Ӱ���ʱ��
					recvtask=new recvTask(pt.sub_fileID);
					recvtimer = new Timer(true);
					recvTimers.put(pt.sub_fileID, recvtimer);
					recvtimer.schedule(recvtask,delay,frequency);
					//��ӿ��ʱ��
					 synchronized(FileSharing.subfileTimers)
					 {
						 if(!FileSharing.subfileTimers.containsKey(fileID_break[0]))
						 {
					    send_FbpTask send_Fbptask =new send_FbpTask(fileID_break[0],pt.m_time,pt.totalsubFiles);
		                Timer subfiletimer = new Timer(true);
		    		    FileSharing.subfileTimers.put(fileID_break[0], subfiletimer);
		    		    //
		    		    subfiletimer.schedule(send_Fbptask,FileSharing.block_time,FileSharing.block_time);
						 }
						 }
					   for(offset=0;offset<lossFiles.size();offset++) 
					    {
					        if(pt.sub_fileID.equals(lossFiles.get(offset)[0].sub_fileID))
					        {   
					        	 //�Ѿ����ǵ�һ���յ����ļ�
					        	 int a=1;
					        	 for(int k=0;k<pktIDs.get(offset).length;k++)
					        	 {
					        		 if(pt.seqno==pktIDs.get(offset)[k])
									   {  
										   System.out.println("���յ���ͬ���Ӱ������ ��"+pt.seqno);
										   a=0;	  
									   } 
					        		 if(pktIDs.get(offset)[k]==-1)
					        		 {
					        			 pktLength=k; 
					        			 break;
					        		 }
					        	 	  
					              }
							   if(a==1)
							   {
							   //���½��յ����Ӱ������б���
							    lossFiles.get(offset)[pktLength]=pt;  //�����б��е�offect������ĵ�pktlength����
							    pktIDs.get(offset)[pktLength++]=pt.seqno;
							   }
					        	break;
					       }
					    }
					   if(offset==lossFiles.size())
					   {
						    System.out.println("��һ�ν��ո��ļ�");
						    Packet []plist=null;
			                int[] subpacketID =null;	    
							int paks=pt.data_blocks;
							subpacketID=new int[paks];
							plist=new Packet[paks];
							for(int k=0;k<paks;k++)
							{
								subpacketID[k]=-1;
							}
							plist[pktLength]=pt;
							subpacketID[pktLength++]=pt.seqno;
							lossFiles.add(plist);
							pktIDs.add(subpacketID);  
							
					   }
					   
					   if(pktLength==lossFiles.get(offset)[0].data_blocks )
					   {
						   System.out.println("������ϣ�"+pt.sub_fileID+",�ܹ����յ��İ���"+pktLength+"��");
						   filesID.add(lossFiles.get(offset)[0].sub_fileID);
						   //ȡ�������ռ�ʱ��
						   if(recvTimers.containsKey(lossFiles.get(offset)[0].sub_fileID))
						     { 
								 message ="����ļ��� "+pt.sub_fileID+" �ļ�ʱ�� ȡ��,���հ����";
								 System.out.println(message);
						    	 recvTimers.get(pt.sub_fileID).cancel();
						    	 recvTimers.remove(pt.sub_fileID);
						     }
						   recvPacket=lossFiles.get(offset);
						   lossFiles.remove(offset);
						   pktIDs.remove(offset);					
	
						   Message msg = new Message();
				           msg .obj = recvPacket;
				           msg .arg1=1;
				           FileSharing.myHandler.sendMessage(msg);
				         //  dealRecvFileMtime(recvPacket[0]);  //������Ϻ������Ӧ�Ĵ���
					   }				   
			   }
			  else
			  {
				 message ="���ļ����Ѿ���ȫ���ܣ�������Ҫ�����İ�";
				 System.out.println(message);
				 synchronized(FileSharing.subfileTimers)
				 {
				   if(FileSharing.subfileTimers.containsKey(fileID_break[0]))
				    {
					 System.out.println("ȡ�����ʱ��");
				//	 FileSharing.writeLog("ȡ�����ʱ��"+"\r\n");
		             FileSharing.subfileTimers.get(fileID_break[0]).cancel();
		             FileSharing.subfileTimers.remove(fileID_break[0]);
				    }
				 }
				 
				 if(recvTimers.containsKey(pt.sub_fileID))
				 {
					 recvTimers.get(pt.sub_fileID).cancel();
		    	     recvTimers.remove(pt.sub_fileID);    
				 }
			  }
	
		    }  
	   }
	  }
	}

	public class recvTask extends java.util.TimerTask
	{
        public String sub_fileID; 
        public Packet[] pkt=null;
        public int[] subSeq=null;
        public int len=0;
		recvTask(String sub_fileID)
		{
		this.sub_fileID=sub_fileID;	
		}
		@Override
		public void run() 
		{
			for(int m=0;m<lossFiles.size();m++)
			{
				if(lossFiles.get(m)[0].sub_fileID.equals(sub_fileID))
				{
					pkt=lossFiles.get(m);
					subSeq=pktIDs.get(m);
					break;
				}
			}
		if(pkt!=null&&subSeq!=null)
		{
			for(int n=0;n<subSeq.length;n++)
			{
				if(subSeq[n]==-1)
        		 {
        			len=n; 
        			break;
        		 }
			}
			int total=pkt[0].data_blocks;
      
          if(len<total)
           {
     		  System.out.println("��ʱ���յ��ļ��� "+sub_fileID+",����"+len+" �� ��");
     		  String[] fileID_break=sub_fileID.split("--");
     		 synchronized(FileSharing.subfileTimers)
    		 {
     		   if(FileSharing.subfileTimers.containsKey(fileID_break[0]))
			    {
	             FileSharing.subfileTimers.get(fileID_break[0]).cancel();
	             FileSharing.subfileTimers.remove(fileID_break[0]);
			    }
    		 }  
			  int lossPkts=total-len ;
			  ArrayList<Integer>losspkts=new  ArrayList<Integer>();
			  losspkts.add(lossPkts);
			  sendFeedBackPack sfb=new sendFeedBackPack(pkt[0].sub_fileID,pkt[0].m_time,losspkts,1);
			  sfb.sendFeedBack();
         } 
		}
	  }
	}
/*
	public void dealRecvFileMtime(Packet pt)
	{
		 File f=new File(pt.filename);
	     DatabaseHelper dbHelper;
		 dbHelper = DatabaseHelper.getDatabaseHelper();
	     if(f.exists())
	     {
	    	 String[] fileInfo=new String[2];
	    	 fileInfo=dbHelper.getFileInfo(pt.filename);
	    	 System.out.println("dbHelper.getFileInfo(pt.filename)  "+fileInfo);
	        if(fileInfo==null)
	        {
	    	 dbHelper.saveFileModifiedTable(pt.filename, pt.m_time, "no");
	        }
	        else if(Long.parseLong(fileInfo[1])<pt.m_time)
	        {
	           dbHelper.deleteFileModified(pt.filename);
	           dbHelper.saveFileModifiedTable(pt.filename, pt.m_time, "no");
	        }
	     } 
	}
	*/
	public void destroy() 
	{
		running=false;
		socket.close();
		filesID.clear();
		lossFiles.clear();
		pktIDs.clear();
		synchronized(recvTimers)
		{
	    if(recvTimers.size()>0)
		 {
		  Iterator it = recvTimers.keySet().iterator();
		  while (it.hasNext())
		   {
		   String key=null;
		   key=(String)it.next();
		   recvTimers.get(key).cancel();  //��ֹ���м�ʱ��������
		   }
		  recvTimers.clear();
		}
	  }	
	}
}

