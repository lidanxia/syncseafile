package com.seafile.sharing;

import java.io.Serializable;
import java.util.ArrayList;

public class FeedBackData implements Serializable
{
	String sub_fileID;
	public ArrayList<Integer> nos=null;  //type=1ʱ����nos[0]�ŵ��Ƕ�ʧ����������type=2ʱ�ŵ��Ƕ�ʧ�����ļ����
    int type=0;
    long curr_time=0;
   	FeedBackData(String fileid,long curr_time,ArrayList<Integer> nos,int type)
   	{
   		this.sub_fileID=fileid;
   		this.nos=new ArrayList<Integer>();
		this.nos=nos;
   		this.type=type;
   		this.curr_time=curr_time;
   	}
}