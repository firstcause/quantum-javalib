package com.denizensoft.oshelper;

import android.webkit.JavascriptInterface;

public interface JsApiInterface
{
	@JavascriptInterface
	public String jsCalcAddStampMinutes(String stTimeStamp, String stMinutes);

	@JavascriptInterface
	public String jsCalcElapsedHours(String stStampAfter, String stStampBefore);

	@JavascriptInterface
	public String jsGetValue(String stTag);

	@JavascriptInterface
	public String jsFormatStamp(String stUtcStamp, String stFormat, String stTimeZone);

	@JavascriptInterface
	public String jsJsonRequest(String stJSON);

	@JavascriptInterface
	public void jsPutValue(String stTag, String stValue);

}
