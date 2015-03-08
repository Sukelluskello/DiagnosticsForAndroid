package com.jheto.diagnostics;

public final class UIDStatConstants {

	public static final int AID_ROOT             = 0;  /* traditional unix root user */

	public static final int AID_SYSTEM        = 1000;  /* system server */
	public static final int AID_RADIO         = 1001;  /* telephony subsystem, RIL */
	public static final int AID_BLUETOOTH     = 1002;  /* bluetooth subsystem */
	public static final int AID_GRAPHICS      = 1003;  /* graphics devices */
	public static final int AID_INPUT         = 1004;  /* input devices */
	public static final int AID_AUDIO         = 1005;  /* audio devices */
	public static final int AID_CAMERA        = 1006;  /* camera devices */
	public static final int AID_LOG           = 1007;  /* log devices */
	public static final int AID_COMPASS       = 1008;  /* compass device */
	public static final int AID_MOUNT         = 1009;  /* mountd socket */
	public static final int AID_WIFI          = 1010;  /* wifi subsystem */
	public static final int AID_ADB           = 1011;  /* android debug bridge (adbd) */
	public static final int AID_INSTALL       = 1012;  /* group for installing packages */
	public static final int AID_MEDIA         = 1013;  /* mediaserver process */
	public static final int AID_DHCP          = 1014;  /* dhcp client */

	public static final int AID_SHELL         = 2000;  /* adb and debug shell user */
	public static final int AID_CACHE         = 2001;  /* cache access */
	public static final int AID_DIAG          = 2002;  /* access to diagnostic resources */

	/* The 3000 series are intended for use as supplemental group id's only. */
	/* They indicate special Android capabilities that the kernel is aware of. */
	public static final int AID_NET_BT_ADMIN  = 3001;  /* bluetooth: create any socket */
	public static final int AID_NET_BT        = 3002;  /* bluetooth: create sco, rfcomm or l2cap sockets */
	public static final int AID_INET          = 3003;  /* can create AF_INET and AF_INET6 sockets */
	public static final int AID_NET_RAW       = 3004;  /* can create raw INET sockets */

	public static final int AID_MISC          = 9998;  /* access to misc storage */
	public static final int AID_NOBODY        = 9999;

	public static final int AID_APP          = 10000; /* first app user */
	
	public final static String getPermName(int perm){
		String output = "";
		if(perm == AID_ROOT) output = "root";      
		else if(perm == AID_SYSTEM) output = "system";    
		else if(perm == AID_RADIO) output = "radio";     
		else if(perm == AID_BLUETOOTH) output = "bluetooth"; 
		else if(perm == AID_GRAPHICS) output = "graphics";  
		else if(perm == AID_INPUT) output = "input";     
		else if(perm == AID_AUDIO) output = "audio";     
		else if(perm == AID_CAMERA) output = "camera";    
		else if(perm == AID_LOG) output = "log";       
		else if(perm == AID_COMPASS) output = "compass";   
		else if(perm == AID_MOUNT) output = "mount";     
		else if(perm == AID_WIFI) output = "wifi";      
		else if(perm == AID_DHCP) output = "dhcp";      
		else if(perm == AID_ADB) output = "adb";       
		else if(perm == AID_INSTALL) output = "install";   
		else if(perm == AID_MEDIA) output = "media";     
		else if(perm == AID_SHELL) output = "shell";     
		else if(perm == AID_CACHE) output = "cache";     
		else if(perm == AID_DIAG) output = "diag";      
		else if(perm == AID_NET_BT_ADMIN) output = "net_bt_admin"; 
		else if(perm == AID_NET_BT) output = "net_bt";    
		else if(perm == AID_INET) output = "inet";       
		else if(perm == AID_NET_RAW) output = "net_raw";   
		else if(perm == AID_MISC) output = "misc";      
		else if(perm == AID_NOBODY) output = "nobody";    
		return output;
	}
	
}
