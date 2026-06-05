/* 
* ==============================================================================================
* 메가펜스 유량제어서비스 Backend Library for JAVA (V26.1) 
* ==============================================================================================
* ## 안내 ##
*   본 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
*   1. 본 라이브러리 소스는 프로젝트 Import용으로 별도의 수정 작업은 필요 없습니다. (필요한 경우 package명 수정 정도)
* 	2. Bootspring등의 java framework를 이용하는 경우 java(controller) 또는 jsp 중에 하나만 적용하세요.(중복 적용 금지)
*	   java framework 환경이라면 java control or interceptor에서 적용을 권장
*      java framework 없는 환경이라면 jsp에서 적용을 권장
* 	3. local pc에서는 정상적으로 동작하지 않을 수 있습니다. SSL(https) 적용된 웹서버에서 테스트를 권장합니다.  
* ----------------------------------------------------------------------------------------------
* ## 주의 ##
*   본 라이브러리는 데브와이 등록 특허 실시의 일부분으로 허가된 고객 및 환경 이외의 이용을 금합니다.
*   무단 열람, 복사, 배포, 수정, 실행, 테스트 등의 행위는 권리침해 사유가 될 수 있습니다.
* ----------------------------------------------------------------------------------------------
* written by ysd@devy.co.kr, (c) 2024 DEVY (https://www.devy.kr)
*/

// 패키지 도메인은 프로젝트 환경에 맞추서 수정하여 import 해도 무방합니다.
package com.devy.megafence;   


import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


/////////////////////////////////
// VER 26.1.605
/////////////////////////////////
public class WebGate {
	
	/**
	 * 대기판정 메인함수 (V26 이후 버전부터는 이 이름 사용 권장)
	 * @param serviceId
	 * @param gateId
	 * @param req
	 * @param res
	 * @return
	 */
	public boolean WG_IsNeedToWait(String serviceId, String gateId, HttpServletRequest req, HttpServletResponse res) {
	    return WG_IsNeedToWaiting(serviceId, gateId, req, res);
	}	
	
	/**
	 * 대기판정 메인함수
	 * @param serviceId
	 * @param gateId
	 * @param req
	 * @param res
	 * @return
	 */
	public boolean WG_IsNeedToWaiting(String serviceId, String gateId, HttpServletRequest req, HttpServletResponse res) {
	    String 			$WG_VER_BACKEND 		= "26.1.605";
		String 			$WG_LANG_BACKEND 		= "JAVA/" + System.getProperty("java.version");
	    String 			$WG_SERVICE_ID 			= serviceId;
	    String 			$WG_GATE_ID 			= gateId;
	    int 			$WG_MAX_TRY_COUNT 		= 3;
	    boolean 		$WG_IS_CHECKOUT_OK 		= false;
	    int 			$WG_GATE_SERVER_MAX 	= 6;
	    List<String> 	$WG_GATE_SERVERS 		= new ArrayList<String>();
	    String 			$WG_TOKEN_NO 			= "";
	    String 			$WG_TOKEN_STATE 		= "";
	    String 			$WG_TOKEN_KEY 			= "";
	    String 			$WG_WAS_IP 				= "";
	    String 			$WG_TRACE 				= "WG_IsNeedToWaiting()::";
	    int 			$WG_TRACE_LEVEL 		= 0;
	    String 			$WG_IS_LOADTEST 		= "N"; // deprecated from V26
	    String 			$WG_REQ_PAGE 			= WG_GetRequstPageUrl(req);
	    String 			$WG_REQ_IP 				= req.getRemoteAddr() != null ? req.getRemoteAddr() : "";
	    String 			$WG_REFERRER 			= WG_GetReferrer(req);
	    String 			$WG_CLIENT_IP 			= WG_GetUserIpAddr(req);
	    int 			$WG_OUT_COUNT 			= 0;
	    int 			$WG_RESULT_CODE 		= 0; 	// 0:OK, 음수:API 호출 실패 등 장애 오류, 양수:업무로직 처리 결과코드
	    String 			$WG_RESULT_MESSAGE 		= "";	// $WG_RESULT_CODE에 대한 내용 
	    String 			$WG_GATE_OPERATION_MODE = "GATE";
	    boolean 		$WG_IS_NEED_TO_WAIT 	= false;
	    boolean 		$WG_RETURN_FLAG   		= false; // 즉시 RETURN 제어용 flag

	    
	    // get trace level from cookie 
	    String traceLevelText = WG_ReadCookie(req, "WG_TRACE_LEVEL");
	    if (traceLevelText != null && !traceLevelText.equals("")) {
	        try {
	            $WG_TRACE_LEVEL = Integer.parseInt(traceLevelText);
	        } catch (Exception ex) {
	            $WG_TRACE_LEVEL = 0;
	        }
	    }

	    // init was ip list
	    for (int i = 0; i < $WG_GATE_SERVER_MAX; i++) {
	        $WG_GATE_SERVERS.add($WG_SERVICE_ID + "-" + i + ".devy.kr");
	    }

	    
	    // deprecated parameter for load test, will be removed in future version
	    if (req.getParameter("IsLoadTest") != null && req.getParameter("IsLoadTest").equals("Y")) {
	        $WG_IS_LOADTEST = "Y";
	    }

	    /******************************************************************************
	     * STEP-1 : URL Parameter로 대기표 검증 (WG_TOKEN url param이 있으면 처리)
	     *******************************************************************************/
	    try {
	        $WG_TRACE += "STEP1:";

	        // WG_TOKEN parsing :  GATE_ID,TOKEN_NO,TOKEN_KEY,WAS_IP
	        String tokenParam = req.getParameter("WG_TOKEN");
	        if (tokenParam != null && tokenParam.length() > 0) {
	            String[] parameterValues = tokenParam.split(",");
	            if (parameterValues.length == 4) {
	                String paramGateId 		= parameterValues[0];
	                String paramTokenNo 	= parameterValues[1];
	                String paramTokenKey 	= parameterValues[2];
	                String paramWasIp 		= parameterValues[3];

	                if (paramTokenNo 	!= null && !paramTokenNo.equals("") &&
	                    paramTokenKey 	!= null && !paramTokenKey.equals("") &&
	                    paramWasIp 		!= null && !paramWasIp.equals("") &&
	                    paramGateId 	!= null && paramGateId.equals($WG_GATE_ID)) 
	                {
	                	
	                	
	                    // API Call by ACTION=OUT
	                    if ($WG_RETURN_FLAG == false) {
	                        $WG_TRACE += "API_OUT:";
	                        String apiUrlText = WG_BuildApiUrl(
	                                paramWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "OUT",
	                                paramTokenNo, paramTokenKey, $WG_CLIENT_IP, $WG_REQ_PAGE, $WG_IS_LOADTEST);

	                        if ($WG_TRACE_LEVEL >= 2) {
	                            $WG_TRACE += apiUrlText + ",";
	                        }

	                        Map<String, Object> json = WG_CallApi(apiUrlText, 10);

	                        $WG_RESULT_CODE 		= WG_GetIntValue(json.get("ResultCode"),0);
	                        $WG_RESULT_MESSAGE 		= WG_GetStringValue(json.get("ResultMessage"));

	                        $WG_TRACE += $WG_RESULT_CODE + "/" + $WG_GATE_OPERATION_MODE;
	                        if ($WG_TRACE_LEVEL >= 2 ) {
	                            $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
	                        }
	                        $WG_TRACE += ",";

	                        // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
	    	                if ($WG_RESULT_CODE == 0) {
	    	                	$WG_WAS_IP				= paramWasIp;
	    	                	$WG_TOKEN_KEY			= paramTokenKey;
		                        $WG_TOKEN_STATE 		= WG_GetStringValue(json.get("TokenState"));
		                        $WG_GATE_OPERATION_MODE = WG_GetStringValue(json.get("GateOperationMode"));
	    	                    $WG_TOKEN_NO 			= WG_GetStringValue(json.get("TokenNo"));
	    	                    $WG_OUT_COUNT 			= WG_GetIntValue(json.get("OutCount"), 0);

	    	                    if ("ALERT".equals($WG_GATE_OPERATION_MODE)) {
	    	                        $WG_TRACE += "ALERT,";
	    	                        $WG_RETURN_FLAG = true;
	    	                        $WG_IS_CHECKOUT_OK = false;
	    	                        $WG_IS_NEED_TO_WAIT = true;
	    	                    } else if ("GATE".equals($WG_GATE_OPERATION_MODE)) {
	    	                    	if("WAIT".equals($WG_TOKEN_STATE)) {
		    	                        $WG_TRACE += "WAIT,";
		    	                        $WG_RETURN_FLAG = true;
		    	                        $WG_IS_CHECKOUT_OK = false;
		    	                        $WG_IS_NEED_TO_WAIT = true;
		    	                    } else if ("CONNECT".equals($WG_TOKEN_STATE)
		    	                            || "IN".equals($WG_TOKEN_STATE)
		    	                            || "OUT".equals($WG_TOKEN_STATE)) {
		    	                        $WG_TRACE += "PASS,";
		    	                        $WG_RETURN_FLAG = true;
		    	                        $WG_IS_CHECKOUT_OK = true;
		    	                        $WG_IS_NEED_TO_WAIT = false;
		    	                    } else {
		    	                        $WG_TRACE += "FAIL1[TokenState:" + $WG_TOKEN_STATE + "],";
		    	                    }
	    	                    } else {
	    	                    	$WG_TRACE += "FAIL2[GateOperationMode:" + $WG_GATE_OPERATION_MODE + "],";
	    	                    }
	    	                } else {
	    	                    $WG_TRACE += "FAIL3[ResultCode:" + $WG_RESULT_CODE + "],";
	    	                }	                        
	                    }
	                } else {
	                    $WG_TRACE += "SKIP1,";
	                }
	            } else {
	                $WG_TRACE += "SKIP2,";
	            }
	        } else {
	            $WG_TRACE += "SKIP3,";
	        }
	    } catch (Exception e) {
	        $WG_TRACE += "ERROR:" + e.getMessage() + ",";
	    }

	    /******************************************************************************
	     * STEP-2 : Cookie로 대기표 검증
	     *******************************************************************************/
	    try {
	        $WG_TRACE += "→STEP2:";

	        if ($WG_RETURN_FLAG == false) {
	            String cookieTokenNo 	= WG_ReadCookie(req, "WG_TOKEN_NO");
	            String cookieTokenKey 	= WG_ReadCookie(req, "WG_CLIENT_ID");
	            String cookieWasIp 		= WG_ReadCookie(req, "WG_WAS_IP");

	            if ($WG_TRACE_LEVEL >= 1 ) {
	                $WG_TRACE += "WG_TOKEN_NO:" + (cookieTokenNo != null ? cookieTokenNo : "NULL") + "|"
	                        + "WG_TOKEN_KEY:" + (cookieTokenKey != null ? cookieTokenKey : "NULL") + "|"
	                        + "WG_WAS_IP:" + (cookieWasIp != null ? cookieWasIp : "NULL") + ",";
	            }

	            // WG_TOKEN_NO, WG_CLIENT_ID, WG_WAS_IP 쿠키가 모두 있을 때 API Call 시도
	            if (cookieTokenNo 	!= null && cookieTokenNo.length() 	> 0 &&
	                cookieTokenKey 	!= null && cookieTokenKey.length() 	> 0 && 
	                cookieWasIp 	!= null && cookieWasIp.length() 	> 0	) 
	            {
	                // API ACTION=OUT
	                if ($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false) {
	                    $WG_TRACE += "API_OUT:";
	                    String apiUrlText = WG_BuildApiUrl(
	                    		cookieWasIp, 
	                            $WG_SERVICE_ID, 
	                            $WG_GATE_ID, 
	                            "OUT",
	                            cookieTokenNo, 
	                            cookieTokenKey, 
	                            $WG_CLIENT_IP, 
	                            $WG_REQ_PAGE, 
	                            $WG_IS_LOADTEST);

	                    if ($WG_TRACE_LEVEL >= 2 ) {
	                        $WG_TRACE += apiUrlText + ",";
	                    }

	                    Map<String, Object> json = WG_CallApi(apiUrlText, 10);

                        $WG_RESULT_CODE 		= WG_GetIntValue(json.get("ResultCode"),0);
                        $WG_RESULT_MESSAGE 		= WG_GetStringValue(json.get("ResultMessage"));

                        $WG_TRACE += $WG_RESULT_CODE + "/" + $WG_GATE_OPERATION_MODE;
                        if ($WG_TRACE_LEVEL >= 2 ) {
                            $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
                        }
                        $WG_TRACE += ",";

                        // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
    	                if ($WG_RESULT_CODE == 0) {
    	                	$WG_WAS_IP				= cookieWasIp;
    	                	$WG_TOKEN_KEY			= cookieTokenKey;
	                        $WG_TOKEN_STATE 		= WG_GetStringValue(json.get("TokenState"));
	                        $WG_GATE_OPERATION_MODE = WG_GetStringValue(json.get("GateOperationMode"));
    	                    $WG_TOKEN_NO 			= WG_GetStringValue(json.get("TokenNo"));
    	                    $WG_OUT_COUNT 			= WG_GetIntValue(json.get("OutCount"), 0);

    	                    
    	                    if ("ALERT".equals($WG_GATE_OPERATION_MODE)) {
    	                        $WG_TRACE += "ALERT,";
    	                        $WG_RETURN_FLAG = true;
    	                        $WG_IS_CHECKOUT_OK = false;
    	                        $WG_IS_NEED_TO_WAIT = true;
    	                    } else if ("GATE".equals($WG_GATE_OPERATION_MODE)) {
    	                    	if("WAIT".equals($WG_TOKEN_STATE)) {
	    	                        $WG_TRACE += "WAIT,";
	    	                        $WG_RETURN_FLAG = true;
	    	                        $WG_IS_CHECKOUT_OK = false;
	    	                        $WG_IS_NEED_TO_WAIT = true;
	    	                    } 
    	                    	else if ("CONNECT".equals($WG_TOKEN_STATE) || "IN".equals($WG_TOKEN_STATE) || "OUT".equals($WG_TOKEN_STATE)) 
    	                    	{
	    	                        $WG_TRACE += "PASS,";
	    	                        $WG_RETURN_FLAG = true;
	    	                        $WG_IS_CHECKOUT_OK = true;
	    	                        $WG_IS_NEED_TO_WAIT = false;
	    	                    } else {
	    	                        $WG_TRACE += "FAIL1[TokenState:" + $WG_TOKEN_STATE + "],";
	    	                    }
    	                    } else {
    	                    	$WG_TRACE += "FAIL2[GateOperationMode:" + $WG_GATE_OPERATION_MODE + "],";
    	                    }
    	                } else {
    	                    $WG_TRACE += "FAIL3[ResultCode:" + $WG_RESULT_CODE + "],";
    	                }	               
	                }
	            } else {
	                $WG_TRACE += "SKIP1,";
	            }
	        } else {
	            $WG_TRACE += "SKIP2,";
	        }
	    } catch (Exception e) {
	        $WG_TRACE += "ERROR:" + e.getMessage() + ",";
	    }


	    /******************************************************************************
	     * STEP-3 : 신규접속자로 간주하고 대기열 표시여부 판단
	     *******************************************************************************/
	    $WG_TRACE += "→STEP3:";

	    int tryCount = 0;

	    if ($WG_RETURN_FLAG == false) {
	        $WG_RESULT_CODE = -1;
	        int serverCount = $WG_GATE_SERVERS.size();
	        int drawResult = new SecureRandom().nextInt(serverCount);

	        // TokenKey 쿠키(WG_CLIENT_ID)가 없으면 신규 발급(채번)
	        $WG_TOKEN_KEY = WG_ReadCookie(req, "WG_CLIENT_ID");
	        if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals("")) 
	        {
	            $WG_TOKEN_KEY = WG_GetRandomString(12);
	            if ($WG_TRACE_LEVEL >= 1 ) 
	            {
	                $WG_TRACE += "Generate TOKEN:" + $WG_TOKEN_KEY + ",";
	            }
	            WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
	        }

	        // 최대 3회 Api call (action=MATCHING) 시도 
	        for (tryCount = 0; tryCount < $WG_MAX_TRY_COUNT; tryCount++) 
	        {
	            try 
	            {
	                $WG_TRACE += "API_MATCHING" + (tryCount + 1) + ":";

	                String serverIp;
	                if (tryCount == 0 && $WG_WAS_IP != null && $WG_WAS_IP.length() > 0) {
	                    serverIp = $WG_WAS_IP;
	                } else {
	                    serverIp = $WG_WAS_IP = $WG_GATE_SERVERS.get((drawResult++) % serverCount);
	                }

	                String apiUrlText = WG_BuildApiUrl(
	                        serverIp, $WG_SERVICE_ID, $WG_GATE_ID, "MATCHING",
	                        "", $WG_TOKEN_KEY, $WG_CLIENT_IP, $WG_REQ_PAGE, $WG_IS_LOADTEST);

	                Map<String, Object> json = WG_CallApi(apiUrlText, 3 * (tryCount + 1));
	                
                    $WG_RESULT_CODE 		= WG_GetIntValue(json.get("ResultCode"),0);
                    $WG_RESULT_MESSAGE 		= WG_GetStringValue(json.get("ResultMessage"));

	                $WG_TRACE += ":" + $WG_RESULT_CODE + "/" + $WG_GATE_OPERATION_MODE + ",";
	                if ($WG_TRACE_LEVEL >= 2 ) {
	                    $WG_TRACE += $WG_RESULT_MESSAGE + ",";
	                }
	                $WG_TRACE += ",";

	                // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
	                if ($WG_RESULT_CODE == 0) 
	                {
                        $WG_TOKEN_STATE 		= WG_GetStringValue(json.get("TokenState"));
                        $WG_GATE_OPERATION_MODE = WG_GetStringValue(json.get("GateOperationMode"));
	                    $WG_TOKEN_NO 			= WG_GetStringValue(json.get("TokenNo"));
	                    $WG_OUT_COUNT 			= WG_GetIntValue(json.get("OutCount"), 0);

	                    if ("ALERT".equals($WG_GATE_OPERATION_MODE)) 
	                    {
	                        $WG_TRACE += "ALERT,";
	                        $WG_RETURN_FLAG = true;
	                        $WG_IS_CHECKOUT_OK = false;
	                        $WG_IS_NEED_TO_WAIT = true;
	                        break;
	                    } 
	                    else if ("GATE".equals($WG_GATE_OPERATION_MODE)) 
	                    {
	                    	if("WAIT".equals($WG_TOKEN_STATE)) {
    	                        $WG_TRACE += "WAIT,";
    	                        $WG_RETURN_FLAG = true;
    	                        $WG_IS_CHECKOUT_OK = false;
    	                        $WG_IS_NEED_TO_WAIT = true;
    	                        break;
    	                    } 
	                    	else if ("CONNECT".equals($WG_TOKEN_STATE)
    	                            || "IN".equals($WG_TOKEN_STATE)
    	                            || "OUT".equals($WG_TOKEN_STATE)) 
	                    	{
    	                        $WG_TRACE += "PASS,";
    	                        $WG_RETURN_FLAG = true;
    	                        $WG_IS_CHECKOUT_OK = true;
    	                        $WG_IS_NEED_TO_WAIT = false;
    	                        break;
    	                    } 
	                    	else 
	                    	{
    	                        $WG_TRACE += "FAIL1[TokenState:" + $WG_TOKEN_STATE + "],";
    	                        break;
    	                    }
	                    } 
	                    else 
	                    {
	                    	$WG_TRACE += "FAIL2[GateOperationMode:" + $WG_GATE_OPERATION_MODE + "],";
	                    }
	                } 
	                else 
	                {
	                    $WG_TRACE += "FAIL3[ResultCode:" + $WG_RESULT_CODE + "],";
	                }	  
	            } 
	            catch (Exception e) 
	            {
	                $WG_TRACE += "ERROR:" + e.getMessage() + ",";
	            }
	        }

	        if (tryCount >= $WG_MAX_TRY_COUNT) 
	        {
	            $WG_TRACE += "RETRY_EXCEEDED,";

	            if ($WG_RESULT_CODE < 0) {
	                $WG_IS_NEED_TO_WAIT = false;
	            } else {
	                $WG_IS_NEED_TO_WAIT = true;
	            }
	        }
	    } 
	    else 
	    {
	        $WG_TRACE += "SKIP,";
	    }

	    $WG_TRACE += "TryCount:" + tryCount + ",";

	    boolean result = $WG_IS_NEED_TO_WAIT;

	    $WG_TRACE += "→returns:" + (result == true ? "true" : "false") + "," + $WG_RESULT_CODE + "," + $WG_RESULT_MESSAGE;
	    $WG_TRACE = WG_LimitTrace($WG_TRACE);
	    

	    /* 항상 생성하는 쿠키 */
	    WG_WriteCookie(res, "WG_TIME", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
	    WG_WriteCookie(res, "WG_GATE_ID", $WG_GATE_ID);
	    WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
	    WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
	    WG_WriteCookie(res, "WG_OUT_COUNT", String.valueOf($WG_OUT_COUNT));

	    /* 조건부 삭제 쿠키 : 존재하는 쿠키만 삭제 (무조건 삭제하지 않음) */
	    WG_DeleteCookie(req, res, "WG_TRACE");
	    WG_DeleteCookie(req, res, "WG_LANG_BACKEND");
	    WG_DeleteCookie(req, res, "WG_VER_BACKEND");
	    WG_DeleteCookie(req, res, "WG_REQ_PAGE");
	    WG_DeleteCookie(req, res, "WG_REQ_IP");
	    WG_DeleteCookie(req, res, "WG_REFERRER");
	    WG_DeleteCookie(req, res, "WG_CLIENT_IP");
	    WG_DeleteCookie(req, res, "WG_TOKEN_STATE");
	    WG_DeleteCookie(req, res, "WG_RESULT_CODE");
	    WG_DeleteCookie(req, res, "WG_GATE_OPERATION_MODE");

	    if($WG_TRACE_LEVEL >= 2)
	    {
		    WG_WriteCookie(res, "WG_TRACE", $WG_TRACE);
		    WG_WriteCookie(res, "WG_REQ_PAGE", $WG_REQ_PAGE);
		    WG_WriteCookie(res, "WG_REQ_IP", $WG_REQ_IP);
		    WG_WriteCookie(res, "WG_GATE_OPERATION_MODE", $WG_GATE_OPERATION_MODE);
		    WG_WriteCookie(res, "WG_REFERRER", $WG_REFERRER);
		    WG_WriteCookie(res, "WG_CLIENT_IP", $WG_CLIENT_IP);
		    WG_WriteCookie(res, "WG_TOKEN_STATE", $WG_TOKEN_STATE);
		    WG_WriteCookie(res, "WG_RESULT_CODE", String.valueOf($WG_RESULT_CODE));
	    }
	    if($WG_TRACE_LEVEL >= 1)
	    {
		    WG_WriteCookie(res, "WG_LANG_BACKEND", $WG_LANG_BACKEND);
		    WG_WriteCookie(res, "WG_VER_BACKEND", $WG_VER_BACKEND);
		    WG_WriteCookie(res, "WG_TRACE", $WG_TRACE);
	    }
	    
	    if($WG_TRACE_LEVEL <= 0) {
	    	WG_DeleteCookie(req, res, "WG_TRACE_LEVEL");
	    }
	    	
	    return result;
	}
	

	/**
	 * 호환성 유지. WG_IsNeedToWaiting()만 사용
	 * @param serviceId
	 * @param gateId
	 * @param req
	 * @param res
	 * @return
	 */
	public boolean WG_IsNeedToWaiting_V2(String serviceId, String gateId, HttpServletRequest req, HttpServletResponse res) 
	{
		return WG_IsNeedToWaiting(serviceId, gateId, req, res);
	}

	
	/**
	 * 호환성 유지. WG_IsNeedToWaiting()만 사용 
	 * @param serviceId
	 * @param gateId
	 * @param req
	 * @param res
	 * @return
	 */
	public boolean WG_IsValidToken(String serviceId, String gateId, HttpServletRequest req, HttpServletResponse res) {
		return !WG_IsNeedToWaiting(serviceId, gateId, req, res);
	}
		
	
	/**
	 * 대기UI HTML return
	 * @param serviceId
	 * @param gateId
	 * @return
	 */
	public String WG_GetWaitingUi(String serviceId, String gateId) {
		return WG_GetWaitingUi(serviceId, gateId, null);
	}

	/**
	 * 대기UI HTML return (개발서버 여부에 따라 CDN URL 분기)
	 * @param serviceId
	 * @param gateId
	 * @param req  null 허용 (null 이면 isDevSite 체크 생략)
	 * @return
	 */
	public String WG_GetWaitingUi(String serviceId, String gateId, HttpServletRequest req) {
		String versionTag = "";
		java.util.Date nowDate = new java.util.Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00");
		versionTag = sdf.format(nowDate);

		// 개발서버 여부 체크 (CDN URL → 로컬 상대경로로 대체)
		String[] devHosts = {"dev.devy.kr"};
		boolean isDevSite = false;
		if (req != null) {
			String host = req.getHeader("Host");
			if (host != null) {
				for (String devHost : devHosts) {
					if (devHost.equals(host)) {
						isDevSite = true;
						break;
					}
				}
			}
		}

		String html = ""
				+ "<!DOCTYPE html>\r\n"
				+ "<html>\r\n"
				+ "<head>\r\n"
				+ "    <meta http-equiv='X-UA-Compatible' content='IE=edge'/>\r\n"
				+ "    <meta charset='utf-8'/>\r\n"
				+ "    <meta http-equiv='cache-control' content='no-cache' />\r\n"
				+ "    <meta http-equiv='Expires' content='-1'/>\r\n"
				+ "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'/>\r\n"
				+ "    <meta name='robots' content='noindex,nofollow'>\r\n"
				+ "    <title></title>\r\n"
				+ "</head>\r\n"
				+ "<body>\r\n"
				+ "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=" + versionTag + "' rel='stylesheet' /> \r\n"
				+ "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=" + versionTag + "'></script>\r\n"
				+ "    <script>\r\n"
				+ "        function WG_PageLoaded() { \r\n"
				+ "            var gateId = 'WG_GATE_ID';\r\n"
				+ "            var nextUrl = window.location.href;\r\n"
				+ "            var config = {\r\n"
				+ "                gateId: gateId,\r\n"
				+ "                uiMode: 'BACKEND',\r\n"
				+ "                topTitle: '서비스 접속 대기 중',\r\n"
				+ "                uiShowDelay: 0 * 1000,\r\n"
				+ "                uiHideDelay: 10 * 1000,\r\n"
				+ "                resetForce: false,\r\n"
				+ "                customType: '',\r\n"
				+ "                onSuccess: function(data) {\r\n"
				+ "                    var res = $WG.lastResponse;\r\n"
				+ "                    if (res.GateOperationMode == 'ALERT') {\r\n"
				+ "                        if (res.GateOperationMessageTitle == 'NOT_OPEN') {\r\n"
				+ "                    			alert(res.GateOperationMessageDetail);\r\n"
				+ "                        }\r\n"
				+ "                    } else if (res.GateOperationMode == 'GATE') {\r\n"
				+ "                        WG_ChangeUrl(nextUrl);\r\n"
				+ "                    }\r\n"
				+ "                },\r\n"
				+ "                onMdsDetected: function(data) {\r\n"
				+ "                    if (typeof turnstile != 'undefined' && turnstile != null) {\r\n"
				+ "                        turnstile.reset();\r\n"
				+ "                    }\r\n"
				+ "                    var msg = '매크로 등에 의한 부정접속 시도가 감지되었습니다.\\n만일 \\'사람인지 확인하십시오\\' 문구가 화면에 표시되었다면 확인란을 체크하신 후 다시 시도해 보시기 바랍니다.';\r\n"
				+ "                    if (data.ResultMessage == 'MDS_FAIL_BLOCKED_IP') {\r\n"
				+ "                        msg = '접속 차단된 IP입니다.\\n다른 IP에서 접속하거나 관리자에게 문의 주시기 바랍니다.';\r\n"
				+ "                    }\r\n"
				+ "                    alert(msg);\r\n"
				+ "                    return;\r\n"
				+ "                },\r\n"
				+ "                onAlert: function(data) {\r\n"
				+ "                    var res = $WG.lastResponse;\r\n"
				+ "                    alert(res.GateOperationMessageDetail);\r\n"
				+ "                },\r\n"
				+ "                onWaiting: function(data) {\r\n"
				+ "                },\r\n"
				+ "                onFail: function(data) {\r\n"
				+ "                    alert('죄송합니다. 잠시 후 다시 시도해 주세요');\r\n"
				+ "                },\r\n"
				+ "                onFinally: function() {\r\n"
				+ "                }\r\n"
				+ "            };\r\n"
				+ "            WG_StartWebGate(config);\r\n"
				+ "        } \r\n"
				+ "    </script>\r\n"
				+ "</body>\r\n"
				+ "</html>";

		if (isDevSite) {
			html = html.replace("https://cdn2.devy.kr/WG_SERVICE_ID", ".");
		}

		return html.replaceAll("WG_SERVICE_ID", serviceId).replaceAll("WG_GATE_ID", gateId);

	}
	
	
	private String WG_LimitTrace(String trace) {
	    if (trace == null) {
	        return "";
	    }

	    if (trace.length() > 1000) {
	        return trace.substring(0, 1000);
	    }

	    return trace;
	}	
	
	
	/**
	 * API URL 생성
	 * @param wasIp
	 * @param serviceId
	 * @param gateId
	 * @param action
	 * @param tokenNo
	 * @param tokenKey
	 * @param clientIp
	 * @param reqPage
	 * @param isLoadTest
	 * @return
	 */
	private String WG_BuildApiUrl(
	        String wasIp,
	        String serviceId,
	        String gateId,
	        String action,
	        String tokenNo,
	        String tokenKey,
	        String clientIp,
	        String reqPage,
	        String isLoadTest) {

	    // null → "" 통일
	    wasIp = (wasIp == null) ? "" : wasIp;
	    serviceId = (serviceId == null) ? "" : serviceId;
	    gateId = (gateId == null) ? "" : gateId;
	    action = (action == null) ? "" : action;
	    tokenNo = (tokenNo == null) ? "" : tokenNo;
	    tokenKey = (tokenKey == null) ? "" : tokenKey;
	    clientIp = (clientIp == null) ? "" : clientIp;
	    reqPage = (reqPage == null) ? "" : reqPage;
	    isLoadTest = (isLoadTest == null) ? "N" : isLoadTest;

	    StringBuilder url = new StringBuilder();
	    url.append("https://").append(wasIp)
	       .append("/?ServiceId=").append(serviceId)
	       .append("&GateId=").append(gateId)
	       .append("&Action=").append(action)
	       .append("&TokenNo=").append(WG_EncodeURIComponent(tokenNo))
	       .append("&TokenKey=").append(WG_EncodeURIComponent(tokenKey))
	       .append("&ClientIp=").append(WG_EncodeURIComponent(clientIp))
	       .append("&ReqPage=").append(WG_EncodeURIComponent(reqPage))
	       .append("&IsLoadTest=").append(isLoadTest);

	    return url.toString();
	}
	
	
	private int WG_GetIntValue(Object value, int defaultValue) {
	    try {
	        if (value == null) {
	            return defaultValue;
	        }
	        if (value instanceof Number) {
	            return ((Number) value).intValue();
	        }
	        return Integer.parseInt(String.valueOf(value));
	    } catch (Exception ex) {
	        return defaultValue;
	    }
	}

	private String WG_GetStringValue(Object value) {
	    return value == null ? "" : String.valueOf(value);
	}

	private String WG_EncodeURIComponent(String value) {
	    try {
	        if (value == null) {
	            return "";
	        }
	        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
	    } catch (Exception ex) {
	        return "";
	    }
	}

	
	/**
	 * Random key 생성 (WG_CLIENT_ID용)
	 * @param length
	 * @return
	 */
	private String WG_GetRandomString(int length) {
	    String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    int charLen = characters.length();
	    StringBuilder randstring = new StringBuilder();
	    SecureRandom random = new SecureRandom();

	    // 기본 길이 12
	    if (length < 12) {
	        length = 12;
	    }

	    // 본문 생성 (뒤 2자리는 체크코드)
	    for (int i = 0; i < (length - 2); i++) {
	        randstring.append(characters.charAt(random.nextInt(charLen)));
	    }

	    // 최소 문자
	    char minChar = randstring.charAt(0);
	    for (int i = 1; i < randstring.length(); i++) {
	        if (randstring.charAt(i) < minChar) {
	            minChar = randstring.charAt(i);
	        }
	    }
	    int minPos = characters.indexOf(minChar);

	    // 최대 문자
	    char maxChar = randstring.charAt(0);
	    for (int i = 1; i < randstring.length(); i++) {
	        if (randstring.charAt(i) > maxChar) {
	            maxChar = randstring.charAt(i);
	        }
	    }
	    int maxPos = characters.indexOf(maxChar);

	    // 체크코드 2자리
	    char minCheckCode = characters.charAt((minPos - 1 + charLen) % charLen);
	    char maxCheckCode = characters.charAt((maxPos + 1 + charLen) % charLen);

	    return randstring.toString() + minCheckCode + maxCheckCode;
	}
	
	
	/**
	 * Read Cookie : URL Encoding 지원 
	 * @param req
	 * @param key
	 * @return
	 */
	private String WG_ReadCookie(HttpServletRequest req, String key) {
	    Cookie[] cookies = req.getCookies();

	    if (cookies != null) {
	        for (int i = 0; i < cookies.length; i++) {
	            if (cookies[i].getName().equals(key)) {
	                try {
	                    return URLDecoder.decode(cookies[i].getValue(), StandardCharsets.UTF_8.name());
	                } catch (Exception ex) {
	                    return cookies[i].getValue();
	                }
	            }
	        }
	    }
	    return "";
	}
	

	/**
	 * Write Cookie : URL Encoding 지원, Lax default
	 * @param res
	 * @param key
	 * @param value
	 */
	private void WG_WriteCookie(HttpServletResponse res, String key, String value) {
	    try {
	        int days = 1;

	        if ("WG_CLIENT_ID".equals(key)) {
	            days = 365;
	        }

	        if (value == null) {
	            value = "";
	        }

	        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());

	        StringBuilder cookieText = new StringBuilder();
	        cookieText.append(key).append("=").append(encodedValue);
	        cookieText.append("; Max-Age=").append(86400 * days);
	        cookieText.append("; Path=/");
	        //cookieText.append("; Secure"); /* local test 지원 */
	        cookieText.append("; SameSite=Lax");
	        //cookieText.append("; HttpOnly"); /* JS 접근 가능해야 함 */
	        res.addHeader("Set-Cookie", cookieText.toString());
	    } catch (Exception ex) {
	        // skip
	    }
	}	
		
	/**
	 * Delete cookie : 존재하는 쿠키만 삭제 (요청에 없는 쿠키는 무조건 삭제하지 않음)
	 * @param req
	 * @param res
	 * @param key
	 */
	private void WG_DeleteCookie(HttpServletRequest req, HttpServletResponse res, String key) {
	    try {
	        // 존재하지 않는 쿠키는 삭제 헤더를 보내지 않음
	        if (!WG_HasCookie(req, key)) {
	            return;
	        }

	        StringBuilder cookieText = new StringBuilder();
	        cookieText.append(key).append("=");
	        cookieText.append("; Max-Age=0");
	        cookieText.append("; Path=/");
	        //cookieText.append("; Secure");
	        cookieText.append("; SameSite=Lax");

	        res.addHeader("Set-Cookie", cookieText.toString());
	    } catch (Exception ex) {
	        // skip
	    }
	}

	/**
	 * 요청에 해당 쿠키가 존재하는지 여부
	 * @param req
	 * @param key
	 * @return
	 */
	private boolean WG_HasCookie(HttpServletRequest req, String key) {
	    Cookie[] cookies = req.getCookies();
	    if (cookies != null) {
	        for (int i = 0; i < cookies.length; i++) {
	            if (cookies[i].getName().equals(key)) {
	                return true;
	            }
	        }
	    }
	    return false;
	}


	/**
	 * API call 후 응답을 json object로
	 * @param urlText
	 * @param timeoutSeconds
	 * @return
	 */
	private Map<String, Object> WG_CallApi(String urlText, int timeoutSeconds) {
	    HttpURLConnection con = null;

	    try {
	        // SSRF check
	        if (!WG_IsValidApiUrl(urlText)) {
	            return WG_MakeErrorMap(-1001, "Invalid API URL");
	        }

	        int timeout = (timeoutSeconds <= 0) ? 10 : timeoutSeconds;
	        int connectTimeout = Math.min(3, timeout);

	        URL url = new URL(urlText);
	        con = (HttpURLConnection) url.openConnection();

	        if (con instanceof HttpsURLConnection) {
	            ((HttpsURLConnection) con).setInstanceFollowRedirects(false);
	        } else {
	            con.setInstanceFollowRedirects(false);
	        }

	        con.setRequestMethod("GET");
	        con.setConnectTimeout(connectTimeout * 1000);
	        con.setReadTimeout(timeout * 1000);
	        con.setUseCaches(false);
	        con.setDoInput(true);
	        con.setRequestProperty("User-Agent", "WebGate-JAVA");
	        con.setRequestProperty("Accept", "application/json,text/plain,*/*");

	        int httpCode = con.getResponseCode();

	        InputStream stream;
	        if (httpCode >= 200 && httpCode < 300) {
	            stream = con.getInputStream();
	        } else {
	            stream = con.getErrorStream();
	        }

	        String responseText = WG_ReadStream(stream);

	        if (httpCode < 200 || httpCode >= 300) {
	            return WG_MakeErrorMap(-1200 - httpCode, "HTTP Error: " + httpCode);
	        }

	        Map<String, Object> json = WG_ParseJson(responseText);

	        if (json == null) {
	            return WG_MakeErrorMap(-1300, "Invalid JSON response");
	        }

	        return json;

	    } catch (java.net.SocketTimeoutException ex) {
	        return WG_MakeErrorMap(-1101, "Timeout: " + ex.getMessage());
	    } catch (javax.net.ssl.SSLHandshakeException ex) {
	        return WG_MakeErrorMap(-1102, "SSL Error: " + ex.getMessage());
	    } catch (Exception ex) {
	        return WG_MakeErrorMap(-1999, "Exception: " + ex.getMessage());
	    } finally {
	        if (con != null) {
	            con.disconnect();
	        }
	    }
	}

	
	private Map<String, Object> WG_MakeErrorMap(int resultCode, String resultMessage) {
	    Map<String, Object> map = new java.util.HashMap<>();
	    map.put("ResultCode", resultCode);
	    map.put("ResultMessage", resultMessage != null ? resultMessage : "");
	    return map;
	}	
	
	private String WG_ReadStream(InputStream stream) {
	    if (stream == null) {
	        return "";
	    }

	    try (InputStream in = stream;
	         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	        byte[] buffer = new byte[4096];
	        int readLen;
	        while ((readLen = in.read(buffer)) != -1) {
	            out.write(buffer, 0, readLen);
	        }

	        return out.toString(StandardCharsets.UTF_8.name());
	    } catch (Exception ex) {
	        return "";
	    }
	}
	
	private String WG_MakeErrorJson(int resultCode, String resultMessage) {
	    String safeMessage = resultMessage;
	    if (safeMessage == null) {
	        safeMessage = "";
	    }

	    safeMessage = safeMessage
	            .replace("\\", "\\\\")
	            .replace("\"", "\\\"")
	            .replace("\r", " ")
	            .replace("\n", " ");

	    return "{\"ResultCode\":" + resultCode + ",\"ResultMessage\":\"" + safeMessage + "\"}";
	}
	
	

	/*
	 * SSRF 대응용 API URL 검증 (V25.1.914)
	 */
	private boolean WG_IsValidApiUrl(String url) {
        if (url == null || url.isEmpty()) 
        	return false;

    	Pattern regEx = Pattern.compile(
    	        /*
    	         * SSRF 방어용 
    	         * https://9000-0.devy.kr/?ServiceId=9000&GateId=1&Action=ACK&TokenNo=69&TokenKey=1MKE8AK4&MdsTurnstileToken=&v=0.02596159270602616
    	         */
    	        "^http[s]?:\\/\\/\\d{4,20}-\\w{1,10}\\.devy\\.kr[\\/,\\?].*$",
    	        Pattern.CASE_INSENSITIVE
    	    );

        return regEx.matcher(url).matches();
    }
    
    
    private static final ObjectMapper WG_OBJECT_MAPPER = new ObjectMapper();

    private Map<String, Object> WG_ParseJson(String responseText) {
        if (responseText == null || responseText.equals("")) {
            return null;
        }

        try {
            return WG_OBJECT_MAPPER.readValue(
                responseText,
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception ex) {
            return null;
        }
    }    
    
    private boolean WG_IsValidIp(String ip) {
        if (ip == null || ip.trim().equals("")) {
            return false;
        }

        String ipv4Pattern =
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        String ipv6Pattern =
            "^[0-9a-fA-F:]+$";

        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }    
    
    private String WG_GetUserIpAddr(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        String xff = req.getHeader("X-Forwarded-For");

        if (xff != null && xff.trim().equals("") == false) {
            String[] ipList = xff.split(",");
            String ip = ipList[0].trim();

            if (WG_IsValidIp(ip)) {
                return ip;
            } else {
                return remoteAddr != null ? remoteAddr : "";
            }
        } else {
            return remoteAddr != null ? remoteAddr : "";
        }
    }
    
    
    private String WG_GetRequstPageUrl(HttpServletRequest req) {
        String scheme = req.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.equals("")) {
            scheme = req.isSecure() ? "https" : "http";
        }

        String host = req.getHeader("Host");
        if (host == null) {
            host = "";
        }

        String uri = req.getRequestURI();
        if (uri == null) {
            uri = "";
        }

        String query = req.getQueryString();
        String fullUrl = scheme + "://" + host + uri;
        if (query != null && query.equals("") == false) {
            fullUrl += "?" + query;
        }

        if (fullUrl.length() > 100) {
            return fullUrl.substring(0, 100);
        }
        return fullUrl;
    }
    
    private String WG_GetReferrer(HttpServletRequest req) {
        String referrer = req.getHeader("Referer");
        if (referrer == null) {
            referrer = "";
        }

        if (referrer.length() > 100) {
            return referrer.substring(0, 100);
        }
        return referrer;
    }
    
}