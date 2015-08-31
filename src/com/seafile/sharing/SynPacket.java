package com.seafile.sharing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
//li,用来传输同步信息的packet
public class SynPacket implements Serializable {
	int type=0;//数据包的类型
	//Map<String,Long> map=new HashMap<String,Long>();
	Map<String,ArrayList<String[]>> map=new LinkedHashMap<String,ArrayList<String[]>>();//发送的数据
	String information=new String();
	SynPacket(int type,Map<String,ArrayList<String[]>> map,String information)
	{
		this.type=type;  //type=1表示全网同步过程正在收集文件信息。type=2表示收到了整理后的表，进入到FileSharing arg=3 case2中执行
		this.map=map;    //FileSharing arg=3 case3为收到nextIP消息的处理
		this.information=information;
	}
	
}
