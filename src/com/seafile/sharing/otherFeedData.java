package com.seafile.sharing;

import java.util.ArrayList;

public class otherFeedData 
{
	    public String sub_fileID;
	    public ArrayList<Integer> nos=null;
	    public long time;
	    long curr_time;
	    otherFeedData (String fileid,ArrayList<Integer> nos,long time,long curr_time)
	    {
	    this.sub_fileID=fileid;
	    this.nos=new ArrayList<Integer>();
		this.nos=nos;
	    this.time=time;
	    this.curr_time=curr_time;
		}
}