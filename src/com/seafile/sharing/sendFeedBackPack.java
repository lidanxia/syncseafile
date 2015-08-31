package com.seafile.sharing;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class sendFeedBackPack
{
	public String id=null;
	public ArrayList<Integer> nos=null;  //丢失的文件块号
	public int type=0;
	long curr_time=0;   //文件的发送时间
	sendFeedBackPack(String fileid,long curr_time,ArrayList<Integer> nos,int type)
	 {
		 this.id=fileid;
		 this.type=type;
		 this.nos=new ArrayList<Integer>();
		 this.nos=nos;
		 this.curr_time=curr_time;
	 }
	public void sendFeedBack()
	{
		boolean isSend=true;
		synchronized(FileSharing.othersFeedpkt)
		{
		for(int k=0;k<FileSharing.othersFeedpkt.size();k++)
		{
		  boolean istrue=FileSharing.othersFeedpkt.get(k).sub_fileID.equals(id);
		 
		    if(istrue)
		    { 
		    	String mess ="其他人已经发送了反馈包";
		    	System.out.println(mess);
		    	if(type==1&&FileSharing.othersFeedpkt.get(k).nos.get(0)>=nos.get(0))
		    		isSend=false;
		    	else if(type==2&&FileSharing.othersFeedpkt.get(k).nos.size()>=nos.size())
				  {
		    		isSend=false;  
				  }
		    }
		    break;
		 }
	   }
		if(isSend)
		{	
		byte[]messages=null;
		Packet FBK=new Packet(1,0,curr_time,0,0,1,0,0,0,false);
	
	    FeedBackData FeedBack=new FeedBackData(id,curr_time,nos,type);
		 try {  
	         ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	         ObjectOutputStream oos = new ObjectOutputStream(baos);  
	         oos.writeObject(FeedBack);
	         messages = baos.toByteArray();   
	         baos.close();  
	         oos.close(); 
		 }
	      catch(Exception e) 
	      {   
	         e.printStackTrace();  
	      } 
		 FBK.sub_fileID=id;
		 FBK.data=messages;
		 Packet[]p=new Packet[1];
		 p[0]=FBK;
		 String mess=null;
		 if(type==1)	 
		     mess ="***发送单个包的反馈包: "+FBK.sub_fileID;
		 else
			 mess="^^^^发送块反馈包: "+FBK.sub_fileID;
		 
	//	FileSharing.writeLog(mess+"\r\n");
		System.out.println(mess);
		FileSharing.sThread.inital(p,FileSharing.bcastaddress,FileSharing.port,0,1,0);
		FileSharing.sThread.sending(); 
		}
	}
	
}

