package com.seafile.sharing;

import java.util.ArrayList;

public class send_FbpTask extends java.util.TimerTask
{
	public String fileID;  //�ļ���ID��
	public ArrayList<Integer>sub_nos=null;  //�յ����ļ��Ŀ��
	public int totalblocks=0;
	private  ArrayList<Integer> miss_nos=new ArrayList<Integer>();  //��ʧ���ļ����
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
				System.out.println(fileID+"-- ��ʧ�˵� "+i+" ��");
		//		 FileSharing.writeLog(fileID+"-��ʱ- ��ʧ�˵� "+i+" ��"+" ms"+"\r\n");
				miss_nos.add(i);
			}
		}
	}
	 public void run() 
  	 { 
	 getMiss_nos();
	 if(miss_nos.size()>0)
	   {
	     System.out.println("��ʱ�����Ϳ鶪ʧ������");
	     sendFeedBackPack sfb=new sendFeedBackPack(fileID,curr_time,miss_nos,2);
         sfb.sendFeedBack();
	   }
  	 }
}
