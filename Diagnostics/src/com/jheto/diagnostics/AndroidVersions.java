package com.jheto.diagnostics;

public final class AndroidVersions {
	
	private AndroidVersions(){}		
	
	//May 2009: Android 1.5.
	public static final int CUPCAKE = 0x00000003;
	
	//September 2009: Android 1.6.
	public static final int DONUT = 0x00000004;
	
	//November 2009: Android 2.0
	public static final int ECLAIR = 0x00000005;
	
	//December 2009: Android 2.0.1
	public static final int ECLAIR_0_1 = 0x00000006;
	
	//January 2010: Android 2.1
	public static final int ECLAIR_MR1 = 0x00000007;
	
	//June 2010: Android 2.2
	public static final int FROYO = 0x00000008;
	
	//November 2010: Android 2.3
	public static final int GINGERBREAD = 0x00000009;
	
	//February 2011: Android 2.3.3.
	public static final int GINGERBREAD_MR1 = 0x0000000a;
	
	//February 2011: Android 3.0.
	public static final int HONEYCOMB = 0x0000000b;
	
	//May 2011: Android 3.1
	public static final int HONEYCOMB_MR1 = 0x0000000c;
	
	//June 2011: Android 3.2.
	public static final int HONEYCOMB_MR2 = 0x0000000d;
	
	//October 2011: Android 4.0.
	public static final int ICE_CREAM_SANDWICH = 0x0000000e;
	
	//December 2011: Android 4.0.3.
	public static final int ICE_CREAM_SANDWICH_MR1 = 0x0000000f;
	
	//June 2012: Android 4.1.
	public static final int JELLY_BEAN = 0x00000010;
	
	//November 2012: Android 4.2, Moar jelly beans!
	public static final int JELLY_BEAN_MR1 = 0x00000011;
	
	//July 2013: Android 4.3, the revenge of the beans.
	public static final int JELLY_BEAN_MR2 = 0x00000012;
	
	//October 2013: Android 4.4, KitKat, another tasty treat.
	public static final int KITKAT = 0x00000013;
	
	//Android 4.4W: KitKat for watches, snacks on the run.
	public static final int KITKAT_WATCH = 0x00000014;
	
	//Lollipop. A flat one with beautiful shadows. But still tasty.
	public static final int LOLLIPOP = 0x00000015;
	
	public static final int getVersion(){
		return android.os.Build.VERSION.SDK_INT;
	}
	
}
