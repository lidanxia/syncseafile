package com.seafile.sharing;

public class SmallFileData 
{
	public String filename;
	public int filelength=0;
	public String sub_fileid;
	long curr_time;
	//String ip;
	boolean islast;
	SmallFileData (String filename,int length,String fileid,long curr_time,boolean islast)
	{
		this.filename=filename;
		this.filelength=length;
		this.sub_fileid=fileid;
		this.curr_time=curr_time;
		//this.ip=ip;
		this.islast=islast;
	}
}
