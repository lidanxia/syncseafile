package com.seafile.sharing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
//li,��������ͬ����Ϣ��packet
public class SynPacket implements Serializable {
	int type=0;//���ݰ�������
	//Map<String,Long> map=new HashMap<String,Long>();
	Map<String,ArrayList<String[]>> map=new LinkedHashMap<String,ArrayList<String[]>>();//���͵�����
	String information=new String();
	SynPacket(int type,Map<String,ArrayList<String[]>> map,String information)
	{
		this.type=type;  //type=1��ʾȫ��ͬ�����������ռ��ļ���Ϣ��type=2��ʾ�յ��������ı����뵽FileSharing arg=3 case2��ִ��
		this.map=map;    //FileSharing arg=3 case3Ϊ�յ�nextIP��Ϣ�Ĵ���
		this.information=information;
	}
	
}
