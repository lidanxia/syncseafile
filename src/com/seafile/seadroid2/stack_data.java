package com.seafile.seadroid2;

public class stack_data 
{
   public String repo_id;
   public String repo_name; 
   public String dir_id;
   public String dir_path;
   stack_data(String repo_id,String repo_name,String dir_id,String dir_path)
   {
	   this.repo_id=repo_id;
	   this.repo_name=repo_name;
	   this.dir_id=dir_id;
	   this.dir_path=dir_path;
   }
}
