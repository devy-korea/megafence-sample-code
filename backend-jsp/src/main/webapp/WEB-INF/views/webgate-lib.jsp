<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
/* 
* ==============================================================================================
* 메가펜스 유량제어서비스 Backend Library for JSP /  V.22.08.17.0
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
* ==============================================================================================
*/
%>
<%@page import="java.io.*"%>
<%@page import="java.io.*"%>
<%@page import="java.io.*"%>
<%@page import="java.net.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.servlet.http.*"%>

<%!
	/* */
	public boolean WG_IsNeedToRedirect(String serviceId, String gateId, HttpServletRequest req, HttpServletResponse res) {
		// begin of declare variable
		String $WG_VERSION = "23.07.21";
		String $WG_MODULE = "Backend/JSP";
		int $WG_MAX_TRY_COUNT = 3; // [fixed] failover api retry count
		int $WG_GATE_SERVER_MAX = 6; // [fixed] was dns record count
		List<String> $WG_GATE_SERVERS = new ArrayList<String>(); // [fixed] 대기표 발급서버 LIST
		for (int i = 0; i < $WG_GATE_SERVER_MAX; i++) {
			$WG_GATE_SERVERS.add(serviceId + "-" + i + ".devy.kr");
		}

		int $serverCount = $WG_GATE_SERVERS.size();
		int $drawResult = new Random().nextInt($WG_GATE_SERVERS.size()) + 0;

		
		int $tryCount = 0;
		// Fail-over를 위해 최대 3차까지 시도
		for ($tryCount = 0; $tryCount < $WG_MAX_TRY_COUNT; $tryCount++) {
			try {
				String wasIp = $WG_GATE_SERVERS.get(($drawResult++) % ($serverCount));
				String apiUrlText = "https://" + wasIp + "/?ServiceId=" + serviceId + "&GateId=" + gateId + "&Action=ASK_NEED_TO_REDIRECT";

				String responseText = WG_CallApi(apiUrlText, 5 * ($tryCount+1));
				//log.info("responseText:" + responseText);

				if (responseText != null && responseText.trim().equalsIgnoreCase("Y")){
					return true;
				}
				else {
					return false;
				}
				
			} catch (Exception $e) {
				// ignore & goto next
			}
		}	
		return false;
	}

	/* 대기여부 판단 */
	public boolean WG_IsNeedToWaiting(String serviceId, String gateId, HttpServletRequest req, HttpServletResponse res) {
		// begin of declare variable
		String $WG_VERSION = "23.02.01";
		String $WG_MODULE = "Backend/JSP";
		String $WG_SERVICE_ID = "0"; // 할당받은 Service ID
		String $WG_GATE_ID = "0"; // 사용할 GATE ID
		int $WG_MAX_TRY_COUNT = 3; // [fixed] failover api retry count
		boolean $WG_IS_CHECKOUT_OK = false; // [fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
		int $WG_GATE_SERVER_MAX = 6; // [fixed] was dns record count.
		List<String> $WG_GATE_SERVERS = new ArrayList<String>(); // [fixed] 대기표 발급서버 LIST
		String $WG_TOKEN_NO = ""; // 대기표 ID
		String $WG_TOKEN_KEY = ""; // 대기표 key
		String $WG_WAS_IP = ""; // 대기표 발급서버
		String $WG_TRACE = ""; // TRACE 정보 (쿠키응답)
		String $WG_IS_LOADTEST = "N"; // jmeter 등으로 발생시킨 요청인지 여부
		String $WG_CLIENT_IP = ""; // 단말 IP (운영자 IP 판단용)
		boolean $WG_IS_TRACE_DETAIL = false; // Detail TRACE 정보 생성여부
		String[] $WG_PROXY = { // proxy 사용하는 경우에만 세팅
				//"10.10.10.10:1000/devy/1", 
				//"10.10.10.10:1000/devy/2", 
				//"10.10.10.10:1000/devy/3"
		};

		HttpServletRequest $REQ;
		HttpServletResponse $RES;
		// end of declare variable

		// begin of declare init variable
		$WG_SERVICE_ID = serviceId;
		$WG_GATE_ID = gateId;
		$REQ = req;
		$RES = res;

		if ($REQ.getParameter("WG_IS_TRACE_DETAIL") != null && $REQ.getParameter("WG_IS_TRACE_DETAIL").equals("Y")) {
			$WG_IS_TRACE_DETAIL = true;
		}

		/* get client ip */
		$WG_CLIENT_IP = $REQ.getRemoteAddr();
		if ($WG_CLIENT_IP == null || $WG_CLIENT_IP.length() == 0) {
			$WG_CLIENT_IP = "N/A";
		}

		/* jmeter 등에서 부하테스트 목적으로 호출 시를 위한 처리 (HttpReqeust URL에 IsLoadTest=Y parameter 추가바랍니다) */
		if ($REQ.getParameter("IsLoadTest") != null && $REQ.getParameter("IsLoadTest").equals("Y")) {
			$WG_IS_LOADTEST = "Y";
		}

		/* init gate server list */
		if ($WG_PROXY == null || $WG_PROXY.length == 0) {
			for (int i = 0; i < $WG_GATE_SERVER_MAX; i++) {
				$WG_GATE_SERVERS.add($WG_SERVICE_ID + "-" + i + ".devy.kr");
			}
		} else {
			for (int i = 0; i < $WG_PROXY.length; i++) {
				$WG_GATE_SERVERS.add($WG_PROXY[i]);
			}
		}

		String cookieGateId = WG_ReadCookie($REQ, "WG_GATE_ID");
		// end of init variable

		//log.info("ServiceId:" + $WG_SERVICE_ID);

		/******************************************************************************
		STEP-1 : URL Prameter로 대기표 검증 (CDN Landing 방식을 이용하는 경우에 해당)
		*******************************************************************************/
		try {
			$WG_TRACE += "STEP1:";

			String tokenParam = $REQ.getParameter("WG_TOKEN");

			if (tokenParam != null && tokenParam.length() > 0) {
				// WG_TOKEN paramter를 '|'로 분리 및 분리된 개수 체크
				String tokenPparamValues[] = tokenParam.split(",");
				if (tokenPparamValues.length == "GATE_ID,TOKEN_NO,TOKEN_KEY,WAS_IP".split(",").length) {
					// WG_TOKEN parameter에 세팅된 값 GET
					$WG_TOKEN_NO = tokenPparamValues[1];
					$WG_TOKEN_KEY = tokenPparamValues[2];
					$WG_WAS_IP = tokenPparamValues[3];
					String paramGateId = tokenPparamValues[0];

					if ($WG_TOKEN_NO != null && $WG_TOKEN_NO.equals("") == false && $WG_TOKEN_KEY != null
							&& $WG_TOKEN_KEY.equals("") == false && $WG_WAS_IP != null && $WG_WAS_IP.equals("") == false
							&& $WG_GATE_ID.equals(paramGateId)) {
						// 대기표 Validation(checkout api call)
						String apiUrlText = "https://" + $WG_WAS_IP + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId="
								+ $WG_GATE_ID + "&Action=OUT&TokenNo=" + $WG_TOKEN_NO + "&TokenKey=" + $WG_TOKEN_KEY
								+ "&ModuleType=" + $WG_MODULE + "&ModuleVersion=" + $WG_VERSION + "&IsLoadTest=" + $WG_IS_LOADTEST;
						if ($WG_IS_TRACE_DETAIL) {
							$WG_TRACE += apiUrlText + "→";
						}

						String responseText = WG_CallApi(apiUrlText, 30);

						if (responseText != null && responseText.indexOf("\"ResultCode\":0") >= 0) {
							$WG_IS_CHECKOUT_OK = true;
							$WG_TRACE += "OK→";
							// cookie set
							WG_WriteCookie($RES, "WG_CLIENT_ID", $WG_TOKEN_KEY);
							WG_WriteCookie($RES, "WG_WAS_IP", $WG_WAS_IP);
							WG_WriteCookie($RES, "WG_TOKEN_NO", $WG_TOKEN_NO);
						} else {
							$WG_TRACE += "FAIL→";
						}
					} else {
						$WG_TRACE += "SKIP1→";
					}
				} else {
					$WG_TRACE += "SKIP2→";
				}
			} else {
				$WG_TRACE += "SKIP3→";
			}
		} catch (Exception $e) {
			$WG_TRACE += "ERROR:" + $e.getMessage() + "→";
			// ignore & goto next
		}
		/* end of STEP-1 */

		/******************************************************************************
		STEP-2 : Cookie로 대기표 검증 (CDN Landing 방식 이외의 일반적인 방식에 해당)
		*******************************************************************************/
		try {
			$WG_TRACE += "STEP2:";

			if ($WG_IS_CHECKOUT_OK == false) {
				// 쿠키값을 읽어서 대기완료한 쿠키인지 체크
				$WG_TOKEN_NO = WG_ReadCookie($REQ, "WG_TOKEN_NO");
				$WG_WAS_IP = WG_ReadCookie($REQ, "WG_WAS_IP");
				$WG_TOKEN_KEY = WG_ReadCookie($REQ, "WG_CLIENT_ID");

				if ($WG_TOKEN_NO == null || $WG_TOKEN_NO.equals("") == true) {
					$WG_TRACE += "$WG_TOKEN_NO is null→";
				}
				if ($WG_WAS_IP == null || $WG_WAS_IP.equals("") == true) {
					$WG_TRACE += "$WG_WAS_IP is null→";
				}
				if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals("") == true) {
					$WG_TRACE += "$WG_TOKEN_KEY is null→";
				}

				if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals("")) {
					$WG_TOKEN_KEY = WG_GetRandomString(8);
					WG_WriteCookie($RES, "WG_CLIENT_ID", $WG_TOKEN_KEY);
				}

				if ($WG_TOKEN_NO != null && $WG_TOKEN_NO.equals("") == false && $WG_TOKEN_KEY != null
						&& $WG_TOKEN_KEY.equals("") == false && $WG_WAS_IP != null && $WG_WAS_IP.equals("") == false
						&& $WG_GATE_ID.equals(cookieGateId)) {

					String apiUrlText = "https://" + $WG_WAS_IP + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId="
							+ $WG_GATE_ID + "&Action=OUT&TokenNo=" + $WG_TOKEN_NO + "&TokenKey=" + $WG_TOKEN_KEY
							+ "&ModuleType=" + $WG_MODULE + "&ModuleVersion=" + $WG_VERSION + "&IsLoadTest=" + $WG_IS_LOADTEST;
					//log.info("apiUrlText:" + apiUrlText);
					if ($WG_IS_TRACE_DETAIL) {
						$WG_TRACE += apiUrlText + "→";
					}

					// 대기표 Validation(checkout api call)
					String responseText = "";
					responseText = WG_CallApi(apiUrlText, 30);
					//log.info("responseText:" + responseText);

					if (responseText != null && responseText.indexOf("\"ResultCode\":0") >= 0) {
						$WG_IS_CHECKOUT_OK = true;
						$WG_TRACE += "OK→";
					} else {
						$WG_TRACE += "FAIL→";
					}
				} else {
					$WG_TRACE += "SKIP→";
				}
			}
		} catch (Exception $e) {
			// ignore & goto next
			$WG_TRACE += "ERROR:" + $e.getMessage() + "→";
		}
		/* end of STEP-2 */

		/******************************************************************************
		 * RETURN
		 *******************************************************************************/
		Boolean $WG_IS_NEED_TO_WAIT = !$WG_IS_CHECKOUT_OK;
		$WG_TRACE += "→return:" + $WG_IS_NEED_TO_WAIT;

		// write cookie for trace
		WG_WriteCookie($RES, "WG_VER_BACKEND", $WG_VERSION);
		WG_WriteCookie($RES, "WG_MOD_BACKEND", $WG_MODULE);
		Date now = new Date();
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); // UTC
		String nowText = sf.format(now);
		WG_WriteCookie($RES, "WG_TIME", nowText);
		WG_WriteCookie($RES, "WG_TRACE", $WG_TRACE);
		WG_WriteCookie($RES, "WG_CLIENT_IP", $WG_CLIENT_IP);
		WG_WriteCookie($RES, "WG_WAS_IP", $WG_WAS_IP);
		WG_WriteCookie($RES, "WG_GATE_ID", $WG_GATE_ID);

		return $WG_IS_NEED_TO_WAIT;
	}

	public String WG_GetWaitingUi(String serviceId, String gateId) {
		String html = "<!DOCTYPE html>\r\n" + "<html>\r\n" + "<head>\r\n"
				+ "    <meta http-equiv='X-UA-Compatible' content='IE=edge'>\r\n" + "    <meta charset='utf-8'>\r\n"
				+ "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>\r\n"
				+ "    <title></title>\r\n"
				+ "    <link href='//cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=220406' rel='stylesheet'>\r\n"
				+ "</head>\r\n" + "<body>\r\n"
				//+ "    <div id='wg-body-wrapper'></div>\r\n"
				+ "    <script type='text/javascript' src='//cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=220406'></script>\r\n"
				+ "    <script>\r\n" + "        window.addEventListener('load', function () {\r\n"
				+ "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload \r\n"
				+ "        });\r\n" + "    </script>\r\n" + "</body>\r\n" + "</html>";

		return html.replaceAll("WG_SERVICE_ID", serviceId).replaceAll("WG_GATE_ID", gateId);

	}

	public String WG_GetRandomString(int length) {
		StringBuffer buffer = new StringBuffer();
		Random random = new Random();

		String chars[] = "1,2,3,4,5,6,7,8,A,B,C,D,E,F,G,H,J,K,L,M,N,P,Q,R,S,T,W,X,Y,Z".split(",");

		for (int i = 0; i < length; i++) {
			buffer.append(chars[random.nextInt(chars.length)]);
		}
		return buffer.toString();
	}

	public String WG_ReadCookie(HttpServletRequest req, String key) {
		Cookie[] cookies = req.getCookies();

		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals(key)) {
					return cookies[i].getValue();
				}
			}
		}
		return null;
	}

	public void WG_WriteCookie(HttpServletResponse res, String key, String value) {
		try {
			String cookieValue = value;
			Cookie cookie = new Cookie(key, URLEncoder.encode(value, "UTF-8"));
			//Cookie cookie = new Cookie(key, value);
			cookie.setMaxAge(86400 * 1);
			cookie.setPath("/");
			res.addCookie(cookie);
		} catch (Exception ex) {
			// skip
		}
	}

	String WG_CallApi(String urlText, int timeoutSeconds) {
		try {
			URL url = new URL(urlText);
			URLConnection con = url.openConnection();
			con.setConnectTimeout(timeoutSeconds); //대기열 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;
			con.setReadTimeout(timeoutSeconds); //대깅려 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;

			BufferedReader buffer = new BufferedReader(new InputStreamReader(con.getInputStream()));

			String lineText = "";
			String responseText = "";
			while ((lineText = buffer.readLine()) != null)
				responseText += lineText;
			buffer.close();

			return responseText;

		} catch (Exception ex) {
			return null;
		}

	}

%>


