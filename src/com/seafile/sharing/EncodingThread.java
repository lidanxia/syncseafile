package com.seafile.sharing;

public class EncodingThread  extends Thread
{
	public byte[]filedata=null;
	public int subFileLength=0;
	public String sub_fileID=null;
	public String filename=null;
	public int totalsubFiles=0;
	public long FileLength=0;
	long curr_time=0;
	EncodingThread(byte[] filedata,int subFileLength,String sub_fileID,String filename,int totalsubFiles,long FileLength,long curr_time)
	{
		   this.filedata=new byte[FileSharing.maxfilelength];
			System.arraycopy(filedata, 0, this.filedata, 0,filedata.length);
			this.subFileLength=subFileLength;
			this.sub_fileID=sub_fileID;
			this.filename=filename;
			this.totalsubFiles=totalsubFiles;
			this.FileLength=FileLength;
			this.curr_time=curr_time;
	}
	public void run()
	{
			Packet[]p=null;
			p=FileSharing.fecfunction.encode(filedata,subFileLength, sub_fileID,filename,totalsubFiles,FileLength,curr_time);
			synchronized(FileSharing.encodedPacket)
			{
				System.out.println("storinginignigg "+sub_fileID);
				if(FileSharing.encodedPacket.size()==FileSharing.stored_blocks)
					FileSharing.encodedPacket.remove(0);
			    FileSharing.encodedPacket.add(p);
			}
	}
 
}
