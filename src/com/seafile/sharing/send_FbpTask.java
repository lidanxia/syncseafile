package com.seafile.sharing;

import java.util.ArrayList;

public class send_FbpTask extends java.util.TimerTask
{
	public String fileID;  //文件的ID号
	public ArrayList<Integer>sub_nos=null;  //收到的文件的块号
	public int totalblocks=0;
	private  ArrayList<Integer> miss_nos=new ArrayList<Integer>();  //丢失的文件块号
	long curr_time=0;
	send_FbpTask(String fileid,long curr_time,int totalblocks)
	{
	  this.fileID=fileid;
	  this.sub_nos=new ArrayList<Integer>();
	  this.totalblocks=totalblocks;
	  this.curr_time=curr_time;
	}
	public void getMiss_nos()
	{
		this.sub_nos=FileSharing.recv_subfiels_no.get(fileID);
		for(int i=0;i<totalblocks;i++)
		{
			if(this.sub_nos!=null&&!this.sub_nos.contains(i)&&!miss_nos.contains(i))
			{
				System.out.println(fileID+"-- 丢失了第 "+i+" 块");
		//		 FileSharing.writeLog(fileID+"-超时- 丢失了第 "+i+" 块"+" ms"+"\r\n");
				miss_nos.add(i);
			}
		}
	}
	 public void run() 
  	 { 
	 getMiss_nos();
	 if(miss_nos.size()>0)
	   {
	     System.out.println("超时，发送块丢失反馈包");
	     sendFeedBackPack sfb=new sendFeedBackPack(fileID,curr_time,miss_nos,2);
         sfb.sendFeedBack();
	   }
  	 }
}
