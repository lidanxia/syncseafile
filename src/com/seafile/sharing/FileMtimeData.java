package com.seafile.sharing;

public class FileMtimeData 
{
	//addQueue�зŵ����ͣ����ͷ���˵����ݣ�ͬ���ǵĽ���������ʽ
   public String filename;
   //public long init_time=0;
   public long mtime=0;
   //public String ip=new String();//*0522
   public boolean islast;//*0522
  // public long stime=0;
   //public int type=4; 
   /* type=0:  ���շ�û�и��ļ�
      type=1:  ���շ��Ѿ����ļ��޸�
      type=2:  �㲥���µģ����շ��յ��������µ��ļ�����
      type=3:  ���������ϵ�ʱ����µ����ظ��ڵ㣬init_time��=0
      type=4:  ��������ȫ��ͬ����init_time==0,��ʱfilename�м�¼���������ļ���filename+ʱ��
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
