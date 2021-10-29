package com.devy.megafence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/* 
* ==============================================================================================
* 메가펜스 유량제어서비스 Backend Library for JAVA / V.21.1.20
* 이 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 수정된 내용은 반드시 공급처에 통보해야 합니다.
* 허가된 고객 및 환경 이외의 열람, 복사, 배포, 수정, 실행, 테스트 등 일체의 이용을 금합니다.
* 작성자 : ysd@devy.co.kr
* All rights reserved to DEVY / https://devy.kr
* ----------------------------------------------------------------------------------------------
* <주의>
* 0. 이 파일은 코드 수정이 필요 없습니다. (import용 library) 
* 1. Bootspring등의 java framework를 이용하는 경우 java(controller) 또는 jsp 중에 하나만 적용하세요.(중복 적용 금지)
*	 java framework 환경이라면 java control에서 적용을 권장
*    java framework 없는 환경이라면 jsp에서 적용을 권장 
* ---------------------------------------------------------------------------------------------
* <이력>
* V.21.1.30 (2021-10-29) 
*   resize default server qty 10 --> 3
*   add cookie WG_GATE_ID, WG_WAS_IP
* V.21.1.20 (2021-09-14) 
*   add client ip parameter in "CHECK" action api (운영자 IP 체크용)
* V.21.1.11 (2021-08-16) 
*   Add Trace API TryCount in STEP-3
* V.21.1.10 (2021-08-08) 
*   WG_TRACE 내용 축소(apiUrl 제거)
*   rename cookie WG_VERSION --> WG_VER_BACKEND
*   add GATE-ID가 일치하는 경우에만 OUT api call (STEP-2)
* V.21.1.4 (2021-08-08)
* 	[minor update] check gateid(from cookie) in STEP1 & STEP2
* 	add cookie function (WG_ReadCookie(), WG_WriteCookie())
* 	improve trace, WG_TRACE
* V.21.1.3 (2021-07-23) 
*   [minor update] auto make $WG_GATE_SERVERS list
*   [minor update] change api protocol http --> https   
* V.21.1.1 (2021-06-29) 
* 	minor fix & 안정화
* 2021-04-03 : UI응답부 template fileload 대체
*              server list update
* 2021-03-24 : response.setContentType() 처리 추가
* 2021-01-20 : 부하발생용 parameter 처리
* 	            api call timeout 1초 --> 2초
* ==============================================================================================
*/

public class WebGate {
	//private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public WebGate()
	{
		// jsp 소스와 동일하게 만들기 위해 Class 기능은 사용하지 않고, 함수 기능 위주로 구현
	}
	
	public boolean WG_IsNeedToWaiting (String serviceId, String gateId,  HttpServletRequest req, HttpServletResponse res) {
		// begin of declare variable
		String  $WG_VERSION            	= "V.21.1.30";           
		String  $WG_SERVICE_ID        	= "0";          				// 할당받은 Service ID
		String  $WG_GATE_ID            	= "0";             			// 사용할 GATE ID
		int     $WG_MAX_TRY_COUNT      	= 3;                    	// [fixed] failover api retry count
		boolean $WG_IS_CHECKOUT_OK     	= false;                	// [fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
		int		$WG_GATE_SERVER_MAX	   	= 3;						// [fixed] was dns record count
		List<String>  $WG_GATE_SERVERS 	= new ArrayList<String>();	// [fixed] 대기표 발급서버 LIST
		String  $WG_TOKEN_NO           	= "";                   	// 대기표 ID
		String  $WG_TOKEN_KEY          	= "";                   	// 대기표 key
		String  $WG_WAS_IP             	= "";                   	// 대기표 발급서버
		String  $WG_TRACE              	= "";                   	// TRACE 정보 (쿠키응답)
		String  $WG_IS_LOADTEST		   	= "N";						// jmeter 등으로 발생시킨 요청인지 여부
		String  $WG_CLIENT_IP			= "";						// 단말 IP (운영자 IP 판단용)

		HttpServletRequest  $REQ ;
	    HttpServletResponse $RES;
	    // end of declare variable
		
	    // begin of declare init variable
		$WG_SERVICE_ID 	= serviceId;
		$WG_GATE_ID 	= gateId;
		$REQ			= req;
		$RES			= res;
		
		/* get client ip */
		$WG_CLIENT_IP = $REQ.getRemoteAddr();
		if($WG_CLIENT_IP == null || $WG_CLIENT_IP.length() == 0)
		{
			$WG_CLIENT_IP = "N/A";
		}
		
		
		/* jmeter 등에서 부하테스트 목적으로 호출 시를 위한 처리 (HttpReqeust URL에 IsLoadTest=Y parameter 추가바랍니다) */
	    if($REQ.getParameter("IsLoadTest") != null && $REQ.getParameter("IsLoadTest").equals("Y"))
	    {
	    	$WG_IS_LOADTEST = "Y"; 
	    }
	    
	    /* init gate server list */
        for(int i=0; i < $WG_GATE_SERVER_MAX; i++) 
        {
            $WG_GATE_SERVERS.add($WG_SERVICE_ID + "-"  + i + ".devy.kr");
        }
		
    	String cookieGateId = WG_ReadCookie($REQ, "WG_GATE_ID");
	    // end of init variable
    	
    	/******************************************************************************
	    STEP-1 : URL Prameter로 대기표 검증 (CDN Landing 방식을 이용하는 경우에 해당)
	    *******************************************************************************/
	    try 
	    {
        	$WG_TRACE += "STEP1:";
	        
        	String tokenParam = $REQ.getParameter("WG_TOKEN");
        	
	        if(tokenParam != null && tokenParam.length() > 0) 
	        {
	            // WG_TOKEN paramter를 '|'로 분리 및 분리된 개수 체크
	            String tokenPparamValues[] = tokenParam.split(",");
	            if (tokenPparamValues.length == "GATE_ID,TOKEN_NO,TOKEN_KEY,WAS_IP".split(",").length)
	            {
	                // WG_TOKEN parameter에 세팅된 값 GET
	                $WG_TOKEN_NO    = tokenPparamValues[1];
	                $WG_TOKEN_KEY   = tokenPparamValues[2];
	                $WG_WAS_IP      = tokenPparamValues[3];
	                String paramGateId = tokenPparamValues[0];

	                if( $WG_TOKEN_NO     != null && $WG_TOKEN_NO.equals("")  == false 
	                    && $WG_TOKEN_KEY != null && $WG_TOKEN_KEY.equals("") == false 
	                    && $WG_WAS_IP    != null && $WG_WAS_IP.equals("")    == false
	                    && $WG_GATE_ID.equals(paramGateId))
	                {
	                    // 대기표 Validation(checkout api call)
	                    String apiUrlText = "https://" + $WG_WAS_IP + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId=" + $WG_GATE_ID + "&Action=OUT&TokenNo=" + $WG_TOKEN_NO + "&TokenKey=" + $WG_TOKEN_KEY + "&IsLoadTest=" + $WG_IS_LOADTEST;
	                    String responseText = WG_CallApi(apiUrlText);
	                
	                    if(responseText != null && responseText.indexOf("\"ResultCode\":0") >= 0)
	                    {
	                        $WG_IS_CHECKOUT_OK = true;
	                        $WG_TRACE += "OK→";
	                    }                    
	                    else {
	                    	$WG_TRACE += "FAIL→";
	                    }
	                }
	                else {
	                	$WG_TRACE += "SKIP1→";
	                }
	            }
	            else {
	            	$WG_TRACE += "SKIP2→";
	            }
	        }
	        else {
	        	$WG_TRACE += "SKIP3→";
	        }
	    }
	    catch(Exception $e) {
	    	$WG_TRACE += "ERROR:" + $e.getMessage() + "→";
	        // ignore & goto next
	    }
	    /* end of STEP-1 */ 

	    /******************************************************************************
	    STEP-2 : Cookie로 대기표 검증 (CDN Landing 방식 이외의 일반적인 방식에 해당)
	    *******************************************************************************/
	    try 
	    {
        	$WG_TRACE += "STEP2:";

        	if($WG_IS_CHECKOUT_OK == false)
	        {
	        	// 쿠키값을 읽어서 대기완료한 쿠키인지 체크
	            $WG_TOKEN_NO  = WG_ReadCookie($REQ, "WG_TOKEN_NO");
	            $WG_WAS_IP    = WG_ReadCookie($REQ, "WG_WAS_IP");
	            $WG_TOKEN_KEY = WG_ReadCookie($REQ, "WG_CLIENT_ID");

	            if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals(""))
	            {
	            	$WG_TOKEN_KEY = WG_GetRandomString(10);
	            	WG_WriteCookie($RES, "WG_CLIENT_ID", $WG_TOKEN_KEY);
	            }
	            
	            if( $WG_TOKEN_NO     != null && $WG_TOKEN_NO.equals("")  == false 
	                && $WG_TOKEN_KEY != null && $WG_TOKEN_KEY.equals("") == false 
	                && $WG_WAS_IP    != null && $WG_WAS_IP.equals("")    == false
	                && $WG_GATE_ID.equals(cookieGateId))
	            {

	            	String apiUrlText = "https://" + $WG_WAS_IP + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId=" + $WG_GATE_ID + "&Action=OUT&TokenNo=" + $WG_TOKEN_NO + "&TokenKey=" + $WG_TOKEN_KEY + "&IsLoadTest=" + $WG_IS_LOADTEST;
	                //logger.info($urlText);

	                // 대기표 Validation(checkout api call)
	            	String responseText = "";
	            	responseText = WG_CallApi(apiUrlText);
	                
	                if(responseText != null && responseText.indexOf("\"ResultCode\":0") >= 0)
	                {
	                    $WG_IS_CHECKOUT_OK = true;
	                    $WG_TRACE += "OK→";
	                }
	                else {
	                	$WG_TRACE += "FAIL→";
	                }
	            }
	            else {
	            	$WG_TRACE += "SKIP→";
	            }
	        }
	    }
	    catch(Exception $e) {
	    	// ignore & goto next
	    	$WG_TRACE += "ERROR:" + $e.getMessage() + "→";
	    }
	    /* end of STEP-2 */ 

	    
	    /******************************************************************************
	    STEP-3 : 대기표가 정상이 아니면(=체크아웃실패) 신규접속자로 간주하고 대기열 표시여부 판단
	             WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출
	    *******************************************************************************/
    	$WG_TRACE += "STEP3:";
	    Boolean $WG_IS_NEED_TO_WAIT = false;
	    if($WG_IS_CHECKOUT_OK == false)
	    {
	        int $serverCount = $WG_GATE_SERVERS.size();
	        int $drawResult = new Random().nextInt($WG_GATE_SERVERS.size()) + 0;

	        int $tryCount = 0;
	        // Fail-over를 위해 최대 3차까지 시도
	        for($tryCount = 0; $tryCount < $WG_MAX_TRY_COUNT; $tryCount++)
	        {
	            try{
	                // WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출 --> json 응답
	                // 임의의 대기열 서버 선택하여 대기상태 확인 (대기해야 하는지 web api로 확인)
	                String serverIp = $WG_GATE_SERVERS.get(($drawResult++)%($serverCount));
	            	String apiUrlText = "https://" + serverIp + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId=" + $WG_GATE_ID + "&Action=CHECK" + "&ClientIp=" + $WG_CLIENT_IP + "&TokenKey=" + $WG_TOKEN_KEY + "&IsLoadTest=" + $WG_IS_LOADTEST;
	            	String responseText = WG_CallApi(apiUrlText);
	            		
	                // 현재 대기자가 있으면 응답문자열에 "WAIT"가 포함, 대기자 수가 없으면 "PASS"가 포함됨
	                if(responseText != null && responseText.length() > 0)
	                {
	                    if(responseText.indexOf("WAIT") >= 0)
	                    {
	                        $WG_TRACE +=  "WAIT,";
	                        $WG_IS_NEED_TO_WAIT = true;
	                        break;
	                    } else { // PASS (대기가 없는 경우)
	                        $WG_TRACE +=  "PASS,";
	                        $WG_IS_NEED_TO_WAIT = false;
	                        break;
	                    }
	                }
	            }catch (Exception $e){
	    	    	// ignore & goto next
	            	$WG_TRACE += "ERROR:" + $e.getMessage() + ",";
	            } 
	        }
	        // 코드가 여기까지 왔다는 것은
	        // 대기열서버응답에 실패 OR 대기자가 없는("PASS") 상태이므로 원래 페이지를 로드합니다.
		    $WG_TRACE += "TryCount:" + $tryCount + ",";
	    }
	    else {
	    	$WG_TRACE += "SKIP,";
	    }
	    /* end of STEP-3 */ 
		
	    
	    $WG_TRACE += "→return:" + $WG_IS_NEED_TO_WAIT;
	    
	    // write cookie for trace
    	WG_WriteCookie($RES, "WG_VER_BACKEND", $WG_VERSION);
    	Date now = new Date();
    	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); //UTC
    	String nowText = sf.format(now);
    	WG_WriteCookie($RES, "WG_TIME", nowText);
    	WG_WriteCookie($RES, "WG_TRACE", $WG_TRACE);
    	WG_WriteCookie($RES, "WG_CLIENT_IP", $WG_CLIENT_IP);
    	WG_WriteCookie($RES, "WG_WAS_IP", $WG_WAS_IP);
    	WG_WriteCookie($RES, "WG_GATE_ID", $WG_GATE_ID);
	    
		return $WG_IS_NEED_TO_WAIT;
	}
	
	public String WG_GetWaitingUi(String serviceId, String gateId) {
		String html = "<!DOCTYPE html>\r\n"
					+ "<html>\r\n"
					+ "<head>\r\n"
					+ "    <meta http-equiv='X-UA-Compatible' content='IE=edge'>\r\n"
					+ "    <meta charset='utf-8'>\r\n"
					+ "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>\r\n"
					+ "    <title></title>\r\n"
					+ "    <link href='//cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css' rel='stylesheet'>\r\n"				
					+ "</head>\r\n"
					+ "<body>\r\n"
					+ "    <div id='wg-body-wrapper'></div>\r\n"
					+ "    <script type='text/javascript' src='//cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=21.1.0'></script>\r\n"
					+ "    <script>\r\n"
					+ "        window.addEventListener('load', function () {\r\n"
					+ "            WG_StartWebGate('WG_GATE_ID', window.location.href); //reload \r\n"
					+ "        });\r\n"
					+ "    </script>\r\n"
					+ "</body>\r\n"
					+ "</html>";
				 
		return html
				.replaceAll("WG_SERVICE_ID", serviceId)
				.replaceAll("WG_GATE_ID", gateId);
		
	}
	

	public String WG_GetRandomString(int length)
	{
	  	StringBuffer buffer = new StringBuffer();
	  	Random random = new Random();
	 
	  	String chars[] = "0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F".split(",");
	 
	  	for (int i=0 ; i<length ; i++)
	  	{
	    	buffer.append(chars[random.nextInt(chars.length)]);
	  	}
	  	return buffer.toString();
	}
	

	public String WG_ReadCookie(HttpServletRequest req, String key) {
        Cookie[] cookies = req.getCookies(); 

        if(cookies != null)
        {
            for(int i= 0; i<cookies.length;i++){
                if(cookies[i].getName().equals(key))       {
                	return cookies[i].getValue(); 
                }
            } 
        }
        return null;
	}
	
	public void WG_WriteCookie(HttpServletResponse res, String key, String value)
	{
		try {
			String cookieValue = value;
			Cookie cookie = new Cookie(key, URLEncoder.encode(value, "UTF-8"));
	    	//Cookie cookie = new Cookie(key, value);
	    	cookie.setMaxAge(86400*1); 
	    	cookie.setPath("/");
	    	res.addCookie(cookie); 
		} 
		catch(Exception ex)
		{
			// skip
		}
	}
	
	
	String WG_CallApi(String urlText)
	{
		try {
	        URL url = new URL(urlText);
	        URLConnection con = url.openConnection();
	        con.setConnectTimeout(2000); //대기열 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;
	        con.setReadTimeout(2000); //대깅려 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;

	        BufferedReader buffer = new BufferedReader(new InputStreamReader(con.getInputStream()));

	        String lineText = "";
	        String responseText = "";
	        while ((lineText = buffer.readLine()) != null)
	    	    responseText += lineText;
	        buffer.close();		
	        
	        return responseText;
			
		}catch(Exception ex)
		{
			return null;
		}
		
	}
}
