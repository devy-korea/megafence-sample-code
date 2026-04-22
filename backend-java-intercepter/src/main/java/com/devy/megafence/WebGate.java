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

/* 
* ==============================================================================================
* 메가펜스 유량제어서비스 Backend Library for JAVA 
* 이 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 수정된 내용은 반드시 공급처에 통보해야 합니다.
* 허가된 고객 및 환경 이외의 열람, 복사, 배포, 수정, 실행, 테스트 등 일체의 이용을 금합니다.
* 작성자 : ysd@devy.co.kr
* All rights reserved to DEVY / https://devy.kr
* ----------------------------------------------------------------------------------------------
* <주의>
* 1. 이 파일은 일반적으로 코드 수정이 필요 없습니다. (단순 import용 library) 
* 2. Bootspring등의 java framework를 이용하는 경우 java(controller) 또는 jsp 중에 하나만 적용하세요.(중복 적용 금지)
*	 java framework 환경이라면 java control에서 적용을 권장
*    java framework 없는 환경이라면 jsp에서 적용을 권장 
* 3. local pc에서는 정상적으로 동작하지 않을 수 있습니다. SSL(https) 적용된 웹서버에서 테스트를 권장합니다.
* * ==============================================================================================
*/


public class WebGate {
	
	////////////////////////////////////////////////
	static final String $WG_VERSION = "26.1.422";
	////////////////////////////////////////////////
	
	//private Logger log = LoggerFactory.getLogger(WebGate.class);

	public WebGate() {
		// jsp 소스와 동일하게 만들기 위해 Class 기능은 사용하지 않고, 함수 기능 위주로 구현
	}

	/**
	 * 대기판정 메인함수 권장이름
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
	public boolean WG_IsNeedToWaiting(String serviceId, String gateId, HttpServletRequest req,
	        HttpServletResponse res) {
	    String $WG_MODULE = "Backend/JAVA";
	    String $WG_SERVICE_ID = serviceId;
	    String $WG_GATE_ID = gateId;
	    int $WG_MAX_TRY_COUNT = 3;
	    boolean $WG_IS_CHECKOUT_OK = false;
	    int $WG_GATE_SERVER_MAX = 6;
	    List<String> $WG_GATE_SERVERS = new ArrayList<String>();

	    String $WG_TOKEN_NO = "";
	    String $WG_TOKEN_STATE = "";
	    String $WG_TOKEN_KEY = "";
	    String $WG_WAS_IP = "";
	    String $WG_TRACE = "WG_IsNeedToWaiting()::";
	    int $WG_TRACE_LEVEL = 0;
	    String $WG_IS_LOADTEST = "N";
	    String $WG_REQ_PAGE = WG_GetRequstPageUrl(req);
	    String $WG_REQ_IP = req.getRemoteAddr() != null ? req.getRemoteAddr() : "";
	    String $WG_REFERRER = WG_GetReferrer(req);
	    String $WG_CLIENT_IP = WG_GetUserIpAddr(req);
	    int $WG_OUT_COUNT = 0;
	    int $WG_RESULT_CODE = 0;
	    String $WG_RESULT_MESSAGE="";
	    boolean $WG_IS_TRACE_DETAIL = false;
	    boolean $WG_IS_NEED_TO_WAIT = false;

	    if (req.getParameter("WG_IS_TRACE_DETAIL") != null
	            && req.getParameter("WG_IS_TRACE_DETAIL").equals("Y")) {
	        $WG_IS_TRACE_DETAIL = true;
	        $WG_TRACE_LEVEL = 2;
	    }

	    String traceLevelText = WG_ReadCookie(req, "WG_TRACE_LEVEL");
	    if (traceLevelText != null && !traceLevelText.equals("")) {
	        try {
	            $WG_TRACE_LEVEL = Integer.parseInt(traceLevelText);
	        } catch (Exception ex) {
	            $WG_TRACE_LEVEL = 0;
	        }
	    }

	    for (int i = 0; i < $WG_GATE_SERVER_MAX; i++) {
	        $WG_GATE_SERVERS.add($WG_SERVICE_ID + "-" + i + ".devy.kr");
	    }

	    if (req.getParameter("IsLoadTest") != null && req.getParameter("IsLoadTest").equals("Y")) {
	        $WG_IS_LOADTEST = "Y";
	    }

	    /******************************************************************************
	     * STEP-1 : URL Parameter로 대기표 검증
	     *******************************************************************************/
	    try {
	        $WG_TRACE += "STEP1:";

	        String tokenParam = req.getParameter("WG_TOKEN");

	        if (tokenParam != null && tokenParam.length() > 0) {
	            String[] parameterValues = tokenParam.split(",");

	            if (parameterValues.length == 4) {
	                String paramGateId = parameterValues[0];
	                String paramTokenNo = parameterValues[1];
	                String paramTokenKey = parameterValues[2];
	                String paramWasIp = parameterValues[3];

	                if (paramTokenNo != null && !paramTokenNo.equals("")
	                        && paramTokenKey != null && !paramTokenKey.equals("")
	                        && paramWasIp != null && !paramWasIp.equals("")
	                        && paramGateId != null && paramGateId.equals($WG_GATE_ID)) {

	                	// API ACTION=ACK
	                	{
	                		$WG_TRACE += "API_ACK:";
		                	// prepare api
			                String apiUrlText = WG_BuildApiUrl(paramWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "ACK", paramTokenNo, paramTokenKey, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);
			                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
			                    $WG_TRACE += apiUrlText + ",";
			                }
	
			                // call api
			                Map<String, Object> json = WG_CallApi(apiUrlText, 5);

			                // api result
			                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
			                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
			                $WG_TOKEN_STATE = WG_GetJsonTokenState(json);

			                $WG_TRACE += $WG_RESULT_CODE + "/" + $WG_TOKEN_STATE;
			                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
			                    $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
			                }
			                $WG_TRACE += ",";

			                if ($WG_RESULT_CODE == 0 && $WG_TOKEN_STATE.equals("WAIT")) {
			                    $WG_TRACE += "OK,";
			                    $WG_IS_CHECKOUT_OK = false;
			                    $WG_IS_NEED_TO_WAIT = true; // 기존 대기 TOKEN 재사용 & 응답종료
			                    $WG_TOKEN_NO = paramTokenNo;
			                    $WG_TOKEN_KEY = paramTokenKey;
			                    $WG_WAS_IP = paramWasIp;
			                    WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
			                    WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
			                    WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
			                } else {
			                    $WG_TRACE += "FAIL1,";
			                }
	                	}
	                	// API ACTION=OUT
	                	if($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false)
	                	{
	                		$WG_TRACE += "API_OUT:";
		                	// prepare api
			                String apiUrlText = WG_BuildApiUrl(paramWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "OUT", paramTokenNo, paramTokenKey, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);
			                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
			                    $WG_TRACE += apiUrlText + ",";
			                }
	
			                // call api
			                Map<String, Object> json = WG_CallApi(apiUrlText, 10);
	
			                // api result
			                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
			                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
			                $WG_TOKEN_STATE = WG_GetJsonTokenState(json);

			                $WG_TRACE += $WG_RESULT_CODE;
			                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
			                    $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
			                }
			                $WG_TRACE += ",";
			                
			                
			                if ($WG_RESULT_CODE == 0) {
			                    $WG_TRACE += "OK,";
			                    $WG_IS_CHECKOUT_OK = true;
			                    $WG_TOKEN_NO = paramTokenNo;
			                    $WG_TOKEN_KEY = paramTokenKey;
			                    $WG_WAS_IP = paramWasIp;
			                    $WG_OUT_COUNT = WG_GetIntValue(json.get("OutCount"), 0);
	
			                    WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
			                    WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
			                    WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
			                } else {
			                    $WG_TRACE += "FAIL2,";
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

	        if ($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false) 
	        {
	            String cookieTokenNo = WG_ReadCookie(req, "WG_TOKEN_NO");
	            String cookieTokenKey = WG_ReadCookie(req, "WG_CLIENT_ID");
	            String cookieWasIp = WG_ReadCookie(req, "WG_WAS_IP");

	            if (cookieTokenKey == null || cookieTokenKey.equals("")) {
	                cookieTokenKey = WG_GetRandomString(12);
	                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
	                    $WG_TRACE += "Generate TOKEN:" + cookieTokenKey + ",";
	                }
	            }

	            if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
	                $WG_TRACE += "WG_TOKEN_NO:" + (cookieTokenNo != null ? cookieTokenNo : "NULL") + "|"
	                        + "WG_TOKEN_KEY:" + (cookieTokenKey != null ? cookieTokenKey : "NULL") + "|"
	                        + "WG_WAS_IP:" + (cookieWasIp != null ? cookieWasIp : "NULL") + ",";
	            }

	            if (cookieTokenNo != null && cookieTokenNo.length() > 0
	                    && cookieTokenKey != null && cookieTokenKey.length() > 0
	                    && cookieWasIp != null && cookieWasIp.length() > 0) {

	            	// API ACTION=ACK
                	{
                		$WG_TRACE += "API_ACK:";
	                	// prepare api
    	                String apiUrlText = WG_BuildApiUrl(cookieWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "ACK", cookieTokenNo, cookieTokenKey, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);
		                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += apiUrlText + ",";
		                }

		                // call api
		                Map<String, Object> json = WG_CallApi(apiUrlText, 5);

		                // api result
		                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
		                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
		                $WG_TOKEN_STATE = WG_GetJsonTokenState(json);

		                $WG_TRACE += $WG_RESULT_CODE + "/" + $WG_TOKEN_STATE;
		                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
		                }
		                $WG_TRACE += ",";

		                if ($WG_RESULT_CODE == 0 && $WG_TOKEN_STATE.equals("WAIT")) {
		                    $WG_TRACE += "OK,";
		                    $WG_IS_CHECKOUT_OK = false;
		                    $WG_IS_NEED_TO_WAIT = true; // 기존 대기 TOKEN 재사용 & 응답종료
		                    $WG_TOKEN_NO = cookieTokenNo;
		                    $WG_TOKEN_KEY = cookieTokenKey;
		                    $WG_WAS_IP = cookieWasIp;
		                    WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
		                    WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
		                    WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
		                } else {
		                    $WG_TRACE += "FAIL1,";
		                }
                	}
                	
                	// API ACTION=OUT
                	if($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false)
                	{	            	
                		$WG_TRACE += "API_OUT:";

                		// prepare api
		                String apiUrlText = WG_BuildApiUrl(cookieWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "OUT", cookieTokenNo, cookieTokenKey, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);
		                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += apiUrlText + ",";
		                }
	
		                // call api
		                Map<String, Object> json = WG_CallApi(apiUrlText, 10);

		                
		                // api result
		                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
		                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
		                $WG_TOKEN_STATE = WG_GetJsonTokenState(json);

		                $WG_TRACE += $WG_RESULT_CODE;
		                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
		                }
		                $WG_TRACE += ",";
	
		                if ($WG_RESULT_CODE == 0) {
		                    $WG_TRACE += "OK,";
		                    $WG_IS_CHECKOUT_OK = true;
		                    $WG_TOKEN_NO = cookieTokenNo;
		                    $WG_TOKEN_KEY = cookieTokenKey;
		                    $WG_WAS_IP = cookieWasIp;
		                    $WG_OUT_COUNT = WG_GetIntValue(json.get("OutCount"), 0);
		                } else {
		                    $WG_TRACE += "FAIL2,";
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
	     * STEP-2B : subdomain cookie로 대기표 검증
	     *******************************************************************************/
	    try {
	        if ($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false) 
	        {
	            String subdomainTokenNo = WG_ReadCookie(req, "WG_TOKEN_NO_S");
	            String subdomainWasIp = WG_ReadCookie(req, "WG_WAS_IP_S");
	            String subdomainTokenKey = WG_ReadCookie(req, "WG_CLIENT_ID_S");
	            String subdomainGateId = WG_ReadCookie(req, "WG_GATE_ID_S");

	            if (subdomainTokenNo != null && subdomainTokenNo.length() > 0
	                    && subdomainWasIp != null && subdomainWasIp.length() > 0
	                    && subdomainTokenKey != null && subdomainTokenKey.length() > 0
	                    && subdomainGateId != null && subdomainGateId.equals($WG_GATE_ID)) {

	            	// API ACTION=ACK
                	{
                		$WG_TRACE += "API_ACK:";
	                	// prepare api
		                String apiUrlText = WG_BuildApiUrl(subdomainWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "ACK", subdomainTokenNo, subdomainTokenKey, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);
		                $WG_TRACE += "APICall:";
		                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += apiUrlText + ",";
		                }

		                // call api
		                Map<String, Object> json = WG_CallApi(apiUrlText, 5);

		                // api result
		                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
		                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
		                $WG_TOKEN_STATE = WG_GetJsonTokenState(json);

		                $WG_TRACE += $WG_RESULT_CODE + "/" + $WG_TOKEN_STATE;
		                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += ":" + $WG_RESULT_MESSAGE + ",";
		                }
		                $WG_TRACE += ",";
		                
		                if ($WG_RESULT_CODE == 0 && $WG_TOKEN_STATE.equals("WAIT")) {
		                    $WG_TRACE += "OK,";
		                    $WG_IS_CHECKOUT_OK = false;
		                    $WG_IS_NEED_TO_WAIT = true; // 기존 대기 TOKEN 재사용 & 응답종료
	                        $WG_TOKEN_NO = subdomainTokenNo;
	                        $WG_TOKEN_KEY = subdomainTokenKey;
	                        $WG_WAS_IP = subdomainWasIp;
		                    WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
		                    WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
		                    WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
		                } else {
		                    $WG_TRACE += "FAIL1,";
		                }
                	}
                	
                	// API ACTION=OUT
                	if($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false)
                	{	            	
                		$WG_TRACE += "API_OUT:";

                		String apiUrlText = WG_BuildApiUrl(subdomainWasIp, $WG_SERVICE_ID, $WG_GATE_ID, "OUT", subdomainTokenNo, subdomainTokenKey, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);
		                if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += "API:" + apiUrlText + ",";
		                }
	
		                // api call
		                Map<String, Object> json = WG_CallApi(apiUrlText, 10);
		                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
		                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
		                
		                $WG_TRACE += $WG_RESULT_CODE;
		                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
		                    $WG_TRACE += ":" + $WG_RESULT_MESSAGE;
		                }
		                $WG_TRACE += ",";
	
	                    if ($WG_RESULT_CODE == 0) {
	                        $WG_TRACE += "OK:subdomain,";
	                        $WG_IS_CHECKOUT_OK = true;
	                        $WG_TOKEN_NO = subdomainTokenNo;
	                        $WG_TOKEN_KEY = subdomainTokenKey;
	                        $WG_WAS_IP = subdomainWasIp;
	                        $WG_OUT_COUNT = WG_GetIntValue(json.get("OutCount"), 0);
	
	                        WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
	                        WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
	                        WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
	                    } else {
	                        $WG_TRACE += "FAIL2:subdomain[" + $WG_RESULT_CODE + "],";
	                    }
                	}
	            } else {
	                $WG_TRACE += "SKIP:subdomain,";
	            }
	        }
	    } catch (Exception e) {
	        $WG_TRACE += "ERROR:subdomain:" + e.getMessage() + ",";
	    }

	    /******************************************************************************
	     * STEP-3 : 신규접속자로 간주하고 대기열 표시여부 판단
	     *******************************************************************************/
	    $WG_TRACE += "→STEP3:";

	    int tryCount = 0;

	    if ($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false) 
	    {
	        $WG_RESULT_CODE = -1;
	        int serverCount = $WG_GATE_SERVERS.size();
	        int drawResult = new SecureRandom().nextInt(serverCount);
	        
	        $WG_TOKEN_KEY = WG_ReadCookie(req, "WG_CLIENT_ID");
	        if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals("")) {
	            $WG_TOKEN_KEY = WG_GetRandomString(12);
	            if ($WG_TRACE_LEVEL >= 1 || $WG_IS_TRACE_DETAIL) {
	                $WG_TRACE += "Generate TOKEN:" + $WG_TOKEN_KEY + ",";
	            }
	            WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);
	        }

	        for (tryCount = 0; tryCount < $WG_MAX_TRY_COUNT; tryCount++) {
	            try {
            		$WG_TRACE += "API_MATCHING" + (tryCount + 1) + ":";

                    String serverIp;
	                if (tryCount == 0 && $WG_WAS_IP != null && $WG_WAS_IP.length() > 0) {
	                    serverIp = $WG_WAS_IP;
	                } else {
	                    serverIp = $WG_WAS_IP = $WG_GATE_SERVERS.get((drawResult++) % serverCount);
	                }

	                String apiUrlText = WG_BuildApiUrl(serverIp, $WG_SERVICE_ID, $WG_GATE_ID, "MATCHING", "", $WG_TOKEN_KEY, $WG_CLIENT_IP,  $WG_REQ_PAGE, $WG_IS_LOADTEST);

	                Map<String, Object> json = WG_CallApi(apiUrlText, 3 * (tryCount + 1));
	                $WG_RESULT_CODE = WG_GetJsonResultCode(json);
	                $WG_RESULT_MESSAGE = WG_GetJsonResultMessage(json);
	                
                    $WG_TRACE += ":" + $WG_RESULT_CODE + ",";
	                if ($WG_TRACE_LEVEL >= 2 || $WG_IS_TRACE_DETAIL) {
	                    $WG_TRACE += $WG_RESULT_MESSAGE + ",";
	                }
	                $WG_TRACE += ",";


	                if ($WG_RESULT_CODE == 0) {
	                    $WG_TOKEN_NO = WG_GetStringValue(json.get("TokenNo"));
	                    $WG_TOKEN_STATE = WG_GetStringValue(json.get("TokenState"));
	                    $WG_OUT_COUNT = WG_GetIntValue(json.get("OutCount"), 0);

	                    if ("WAIT".equals($WG_TOKEN_STATE)) {
	                        $WG_TRACE += "WAIT,";
	                        $WG_IS_NEED_TO_WAIT = true;
	                        break;
	                    } else if ("CONNECT".equals($WG_TOKEN_STATE)
	                            || "IN".equals($WG_TOKEN_STATE)
	                            || "OUT".equals($WG_TOKEN_STATE)) {
	                        $WG_TRACE += "PASS,";
	                        $WG_IS_NEED_TO_WAIT = false;
	                        break;
	                    } else {
	                        $WG_TRACE += "ERROR_STATE:" + $WG_TOKEN_STATE + ",";
	                        continue;
	                    }
	                } else {
	                    $WG_TRACE += "FAIL1[" + $WG_RESULT_CODE + "],";
	                    continue;
	                }
	            } catch (Exception e) {
	                $WG_TRACE += "ERROR:" + e.getMessage() + ",";
	            }
	        }

	        if (tryCount >= $WG_MAX_TRY_COUNT) {
	            $WG_TRACE += "RETRY_EXCEEDED,";

	            if ($WG_RESULT_CODE < 0) { // HTTP 통신 오류 등 외부 오류이면 bypass
	                $WG_IS_NEED_TO_WAIT = false;
	            } else {
	                $WG_IS_NEED_TO_WAIT = true;
	            }
	        }
	    } else {
	        $WG_TRACE += "SKIP,";
	    }

	    $WG_TRACE += "TryCount:" + tryCount + ",";

	    boolean result;
	    if ($WG_IS_CHECKOUT_OK || !$WG_IS_NEED_TO_WAIT) {
	        result = false;
	    } else {
	        result = true;
	    }

	    $WG_TRACE += "→returns:" + (result == true ? "true":"false") + "," + $WG_RESULT_CODE + "," + $WG_RESULT_MESSAGE;
	    $WG_TRACE = WG_LimitTrace($WG_TRACE);
	    
	    WG_WriteCookie(res, "WG_TRACE", $WG_TRACE);
	    WG_WriteCookie(res, "WG_MOD_BACKEND", $WG_MODULE);
	    WG_WriteCookie(res, "WG_VER_BACKEND", $WG_VERSION);
	    WG_WriteCookie(res, "WG_TIME", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
	    WG_WriteCookie(res, "WG_GATE_ID", $WG_GATE_ID);
	    WG_WriteCookie(res, "WG_WAS_IP", $WG_WAS_IP);
	    WG_WriteCookie(res, "WG_REQ_PAGE", $WG_REQ_PAGE);
	    WG_WriteCookie(res, "WG_REQ_IP", $WG_REQ_IP);
	    WG_WriteCookie(res, "WG_REFERRER", $WG_REFERRER);
	    WG_WriteCookie(res, "WG_CLIENT_IP", $WG_CLIENT_IP);
	    WG_WriteCookie(res, "WG_TOKEN_NO", $WG_TOKEN_NO);
	    WG_WriteCookie(res, "WG_TOKEN_STATE", $WG_TOKEN_STATE);
	    WG_WriteCookie(res, "WG_OUT_COUNT", String.valueOf($WG_OUT_COUNT));
	    WG_WriteCookie(res, "WG_RESULT_CODE", String.valueOf($WG_RESULT_CODE));
	    WG_WriteCookie(res, "WG_TRACE_LEVEL", "0"); // intentional reset after each request
	    //WG_WriteCookie(res, "WG_CLIENT_ID", $WG_TOKEN_KEY);

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
		String versionTag = "";
		java.util.Date nowDate = new java.util.Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00");
		versionTag = sdf.format(nowDate);		
		
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
				+ "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=" + versionTag + "' rel='stylesheet'>\r\n"
				+ "</head>\r\n" 
				+ "<body>\r\n"
				+ "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=" + versionTag + "'></script>\r\n"
				+ "    <script>\r\n" 
				//+ "        document.addEventListener('DOMContentLoaded', function () {\r\n"
				//+ "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload \r\n"
				//+ "        });\r\n"
				+ "        //V25.1.827 \r\n"
				+ "        function WG_PageLoaded() { \r\n"
				+ "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload \r\n"
				+ "        } \r\n"
				+ "    </script>\r\n" 
				+ "</body>\r\n" 
				+ "</html>";

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
	        cookieText.append("; Secure");
	        cookieText.append("; SameSite=Lax");

	        // PHP 최종소스 기준 httponly=false 이므로 HttpOnly는 넣지 않음
	        res.addHeader("Set-Cookie", cookieText.toString());
	    } catch (Exception ex) {
	        // skip
	    }
	}	
		
	/**
	 * Delete cookie
	 * @param res
	 * @param key
	 */
	private void WG_DeleteCookie(HttpServletResponse res, String key) {
	    try {
	        StringBuilder cookieText = new StringBuilder();
	        cookieText.append(key).append("=");
	        cookieText.append("; Max-Age=0");
	        cookieText.append("; Path=/");
	        cookieText.append("; Secure");
	        cookieText.append("; SameSite=Lax");

	        res.addHeader("Set-Cookie", cookieText.toString());
	    } catch (Exception ex) {
	        // skip
	    }
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
	
	/**
	 * get json.ResultCode 
	 * @param json
	 * @return
	 */
	private int WG_GetJsonResultCode(Map<String, Object> json) {
	    if (json == null) {
	        return -1;
	    }
	    return WG_GetIntValue(json.get("ResultCode"), -1);
	}

	/**
	 * get json.ResultMessage
	 * @param json
	 * @return
	 */
	private String WG_GetJsonResultMessage(Map<String, Object> json) {
	    if (json == null) {
	        return "";
	    }
	    return WG_GetStringValue(json.get("ResultMessage"));
	}	
	
	/**
	 * get json.TokenState (있다면)
	 * @param json
	 * @return
	 */
	private String WG_GetJsonTokenState(Map<String, Object> json) {
	    if (json == null) {
	        return "";
	    }
	    return WG_GetStringValue(json.get("TokenState"));
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