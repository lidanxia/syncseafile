package com.seafile.sharing;

public class FileMtimeData 
{
	//addQueue中放的类型，发送服务端的数据，同步是的交互数据形式
   public String filename;
   //public long init_time=0;
   public long mtime=0;
   //public String ip=new String();//*0522
   public boolean islast;//*0522
  // public long stime=0;
   //public int type=4; 
   /* type=0:  接收方没有该文件
      type=1:  接收方已经将文件修改
      type=2:  广播最新的，接收方收到后发送最新的文件即可
      type=3:  将服务器上的时间更新到本地各节点，init_time！=0
      type=4:  正常发起全网同步，init_time==0,此时filename中记录的是所有文件的filename+时间
    */
   public FileMtimeData(String filename,long mtime,boolean islast)
    {
	   this.filename=filename;
	   this.mtime=mtime;
	   //this.ip=ip;//*0522
	   this.islast=islast;//*0522
	  // this.stime=serv_time;
	   //this.init_time=init_time;
	   //this.type=type;
	}
}
