package com.seafile.sharing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

import com.seafile.seadroid2.BrowserActivity;
import com.seafile.seadroid2.data.DatabaseHelper;

/*
 public class RecvServerTime extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public boolean running=true;
	public String recvIP=null;
	byte[]messages=null;
	public void init()
	{
		try {	
			socket= new DatagramSocket(null); 
			socket.setReuseAddress(true); 
			socket.bind(new InetSocketAddress(FileSharing.FileMtimePort)); 
			socket.setBroadcast(true);		
		    messages=new byte[1024];
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	public void run()
	{
		while(running)
		{
			 packet = new DatagramPacket(messages, messages.length);
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
				
			  if (!recvIP.equals(BrowserActivity.getIp())&& packet.getData().length != 0) 
			     {
				  FileMtimeData pt=null;
				                //  接收不为空，且不是自己的包 
				   try
				   {
					   ByteArrayInputStream ba=new ByteArrayInputStream(packet.getData());
					   ObjectInputStream oi=new ObjectInputStream(ba);
					   pt=(FileMtimeData)oi.readObject();
					   ba.close();
					   oi.close();
				   }catch(Exception e)
				   {
					   System.out.println(e.toString());
					    e.printStackTrace();
				   }
				   DatabaseHelper dbHelper;
				  dbHelper = DatabaseHelper.getDatabaseHelper();
				  switch(pt.type)
				  {
				  case 0:
				      break;
				  case 1:
					  break;
				  case 2:
					  break;
				  case 3:
					  String[] fileinfo=dbHelper.getFileInfo(pt.filename);
					  if(fileinfo!=null)
					  {
					   long curr_time=Long.parseLong(fileinfo[1]);
					   if(pt.init_time==curr_time)
					   {
						  dbHelper.deleteFileModified(pt.filename);
						  FileSharing.addFileModified(pt.filename,pt.mtime,"no");
					   }
					  }
					  break;
				  case 4:
					  recvAllSync(pt, dbHelper);
					  break;
				  }
				   
			    }
		}
	
	}

	public void recvAllSync(FileMtimeData pt, DatabaseHelper dbHelper)
	{
		 String localFiles= FileSharing.getAllLocalFiles();
			String[]lofiles=localFiles.split("\\|");
			String recvInfos=pt.filename;
			String[] recvinfo=recvInfos.split("\\|");
			ArrayList<String>self_files=new ArrayList<String>();      //本机独有的文件
			ArrayList<String>shortof_files=new ArrayList<String>();   //本机缺少的文件
			ArrayList<String> update_time=new ArrayList<String>();   //自己的文件比较新的列表
			int[] equalId=new int[lofiles.length];
		 
			boolean isEqual=false;
			int l=0;
			for(int j=0;j<recvinfo.length;j++)
			{ 
				recvinfo[j].trim();
				String[] recvs=recvinfo[j].split("*");
				recvs[0].trim();  //文件名
				recvs[1].trim();  //修改时间
			    for(int i=0;i<lofiles.length;i++)
			    {
					isEqual=false;
					lofiles[i].trim();
					if(recvinfo[j]!="" &&recvs[0].equals(lofiles[i]))
					{
						isEqual=true;
						equalId[l++]=i; //相等的记入一个链表，下面用来计算自己独有的。		
						String []fileinfos=dbHelper.getFileInfo(lofiles[i]);
						if(fileinfos!=null&&Long.parseLong(fileinfos[1])>Long.parseLong(recvs[1]))
						{
							//自己的文件修改时间较新，加入到队列---文件+时间
							update_time.add(lofiles[i]);
						}
						break;		
					}
				}
				if(isEqual==false)	
					shortof_files.add(recvs[0]);  //本机缺少的文件
			}
			boolean equal=false;
			 for(int k=0;k<lofiles.length;k++)
			  {
				 equal=false;
				 for(int p=0;p<equalId.length;p++)
				 {
					 if(k==equalId[p])
					 {
						 equal=true;
						 break;
					 }
				 }
				 if(equal==false)
					 self_files.add(lofiles[k]);
			  }
			 for(int t=0;t<shortof_files.size();t++)
			 {
		      FileMtimeData fm=new FileMtimeData();
			 }
		
	}

}	*/
