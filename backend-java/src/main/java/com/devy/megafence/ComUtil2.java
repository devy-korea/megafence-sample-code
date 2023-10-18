package com.devy.megafence; // 패키지 도메인은 프로젝트 환경에 맞추서 수정하여 import 해도 무방합니다.

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ComUtil2 {
	private Logger log = LoggerFactory.getLogger(ComUtil.class);

	public ComUtil2() {
		// jsp 소스와 동일하게 만들기 위해 Class 기능은 사용하지 않고, 함수 기능 위주로 구현
	}

	/*
	 * #############################################################################
	 * 업데이트(B안) 안내
	 * #############################################################################
	 * 
	 * 최신 안정화 모듈(V.23) 기반으로 업데이트 버전에 SecureRandom() 적용한 버젼입니다.
	 * 
	 * A안 변경사항 동일 적용 
	 * 		1. Random() --> SecureRandom()으로 함수 변경 
	 * 		2. API SERVER IP 배열(WG_GATE_SERVERS) 축소 : 12개 --> 6개 (서비스 성능 영향 없음) 
	 * 		3. API Protocol 변경 : http --> https
	 * 
	 * V.21 --> V.23 주요 변경내용 
	 * 		1. wgGetRandomString(), wgReadCookie(), wgWriteCookie(), wgCallApi() 함수 추가 
	 * 		2. isWatingMegaFence() 실행 시 WG_로 시작하는 몇몇 쿠키 생성 (ex: WG_CLIENT_ID, ..) 
	 * 			: 동시접속자 집계기능 제공 및 Trace 기능 용도
	 *		3. 기타 안정화
	 *
	 * -----------------------------------------------------------------------------------
	 * <업데이트 방법> 
	 * -----------------------------------------------------------------------------------
	 * 		isWatingMegaFence() 함수 교체 및 함수 추가 
	 * 			: 아래 소스 60 ~ 330 line 해당
	 */

	/*
	 * 대기열 체크 (대기여부 판단)
	 */
	public static boolean isWatingMegaFence(HttpServletRequest request, HttpServletResponse response, String gateId)
			throws Exception {

		String $WG_VERSION = "23.09.10";
		String $WG_MODULE = "Backend/JAVA";
		String $WG_SERVICE_ID = "1011"; // fixed
		String $WG_GATE_ID = gateId; // 사용할 GATE ID from param
		int $WG_MAX_TRY_COUNT = 3; // [fixed] failover api retry count
		boolean $WG_IS_CHECKOUT_OK = false; // [fixed] 대기를 완료한 정상 대기표 여부 (true : 대기완료한 정상 대기표, false : 정상대기표 아님)
		int $WG_GATE_SERVER_MAX = 6; // [fixed] was dns record count
		List<String> $WG_GATE_SERVERS = new ArrayList<String>(); // [fixed] 대기표 발급서버 LIST
		String $WG_TOKEN_NO = ""; // 대기표 ID
		String $WG_TOKEN_KEY = ""; // 대기표 key
		String $WG_WAS_IP = ""; // 대기표 발급서버
		String $WG_TRACE = ""; // TRACE 정보 (쿠키응답)
		String $WG_IS_LOADTEST = "N"; // jmeter 등으로 발생시킨 요청인지 여부
		String $WG_CLIENT_IP = ""; // 단말 IP (운영자 IP 판단용)
		boolean $WG_IS_TRACE_DETAIL = false; // Detail TRACE 정보 생성여부

		HttpServletRequest $REQ;
		HttpServletResponse $RES;
		// end of declare variable

		// begin of declare init variable
		$REQ = request;
		$RES = response;

		if ($REQ.getParameter("WG_IS_TRACE_DETAIL") != null && $REQ.getParameter("WG_IS_TRACE_DETAIL").equals("Y")) {
			$WG_IS_TRACE_DETAIL = true;
		}

		/* get client ip */
		$WG_CLIENT_IP = $REQ.getRemoteAddr();
		if ($WG_CLIENT_IP == null || $WG_CLIENT_IP.length() == 0) {
			$WG_CLIENT_IP = "N/A";
		}

		/*
		 * jmeter 등에서 부하테스트 목적으로 호출 시를 위한 처리 (HttpReqeust URL에 IsLoadTest=Y parameter
		 * 추가바랍니다)
		 */
		if ($REQ.getParameter("IsLoadTest") != null && $REQ.getParameter("IsLoadTest").equals("Y")) {
			$WG_IS_LOADTEST = "Y";
		}

		/* init gate server list */
		for (int i = 0; i < $WG_GATE_SERVER_MAX; i++) {
			$WG_GATE_SERVERS.add($WG_SERVICE_ID + "-" + i + ".devy.kr");
		}

		String cookieGateId = wgReadCookie($REQ, "WG_GATE_ID");
		// end of init variable

		// log.info("ServiceId:" + $WG_SERVICE_ID);

		/******************************************************************************
		 * STEP-1 : URL Prameter로 대기표 검증 (CDN Landing 방식을 이용하는 경우에 해당)
		 *******************************************************************************/
		// 미사용

		/******************************************************************************
		 * STEP-2 : Cookie로 대기표 검증 (CDN Landing 방식 이외의 일반적인 방식에 해당)
		 *******************************************************************************/
		try {
			$WG_TRACE += "STEP2:";

			if ($WG_IS_CHECKOUT_OK == false) {
				// 쿠키값을 읽어서 대기완료한 쿠키인지 체크
				$WG_TOKEN_NO = wgReadCookie($REQ, "WG_TOKEN_NO");
				$WG_WAS_IP = wgReadCookie($REQ, "WG_WAS_IP");
				$WG_TOKEN_KEY = wgReadCookie($REQ, "WG_CLIENT_ID");

				if ($WG_TOKEN_NO == null || $WG_TOKEN_NO.equals("") == true) {
					$WG_TRACE += "$WG_TOKEN_NO is null→";
				}

				if ($WG_WAS_IP == null || $WG_WAS_IP.equals("") == true) {
					$WG_TRACE += "$WG_WAS_IP is null→";
				}
				// SSRF 대응 : was ip가 devy.kr로 끝나지 않으면 무효화
				else if (!$WG_WAS_IP.toLowerCase().endsWith(".devy.kr")) {
					$WG_WAS_IP = "";
					$WG_TRACE += "Invalid $WG_WAS_IP(SSRF)→";
				}

				if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals("") == true) {
					$WG_TRACE += "$WG_TOKEN_KEY is null→";
				}

				if ($WG_TOKEN_KEY == null || $WG_TOKEN_KEY.equals("")) {
					$WG_TOKEN_KEY = wgGetRandomString(8);
					wgWriteCookie($RES, "WG_CLIENT_ID", $WG_TOKEN_KEY);
				}

				if ($WG_TOKEN_NO != null && $WG_TOKEN_NO.equals("") == false && $WG_TOKEN_KEY != null
						&& $WG_TOKEN_KEY.equals("") == false && $WG_WAS_IP != null && $WG_WAS_IP.equals("") == false
						&& $WG_GATE_ID.equals(cookieGateId)) {

					String apiUrlText = "https://" + $WG_WAS_IP + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId="
							+ $WG_GATE_ID + "&Action=OUT&TokenNo=" + $WG_TOKEN_NO + "&TokenKey=" + $WG_TOKEN_KEY
							+ "&ModuleType=" + $WG_MODULE + "&ModuleVersion=" + $WG_VERSION + "&IsLoadTest="
							+ $WG_IS_LOADTEST;
					// log.info("apiUrlText:" + apiUrlText);
					if ($WG_IS_TRACE_DETAIL) {
						$WG_TRACE += apiUrlText + "→";
					}

					// 대기표 Validation(checkout api call)
					String responseText = "";
					apiUrlText = XSS_ATTACK_REPLACE_SET(apiUrlText);
					responseText = wgCallApi(apiUrlText, 20);
					// log.info("responseText:" + responseText);

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
		 * STEP-3 : 대기표가 정상이 아니면(=체크아웃실패) 신규접속자로 간주하고 대기열 표시여부 판단 WG_GATE_SERVERS 서버
		 * 중임의의 서버에 API 호출
		 *******************************************************************************/

		$WG_TRACE += "STEP3:";
		Boolean $WG_IS_NEED_TO_WAIT = false;
		if ($WG_IS_CHECKOUT_OK == false) {
			int $serverCount = $WG_GATE_SERVERS.size();
			int $drawResult = new SecureRandom().nextInt($WG_GATE_SERVERS.size()) + 0;

			int $tryCount = 0;
			// Fail-over를 위해 최대 3차까지 시도
			for ($tryCount = 0; $tryCount < $WG_MAX_TRY_COUNT; $tryCount++) {
				try {
					// WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출 --> json 응답
					if ($tryCount == 0 && $WG_WAS_IP != null && $WG_WAS_IP.length() > 0) {
						// 최초1회는 cookie의 wasip 사용
					} else {
						// 임의의 대기열 서버 선택하여 대기상태 확인 (대기해야 하는지 web api로 확인)
						$WG_WAS_IP = $WG_GATE_SERVERS.get(($drawResult++) % ($serverCount));
					}

					String apiUrlText = "https://" + $WG_WAS_IP + "/?ServiceId=" + $WG_SERVICE_ID + "&GateId="
							+ $WG_GATE_ID + "&Action=CHECK" + "&ClientIp=" + $WG_CLIENT_IP + "&TokenKey="
							+ $WG_TOKEN_KEY + "&ModuleType=" + $WG_MODULE + "&ModuleVersion=" + $WG_VERSION
							+ "&IsLoadTest=" + $WG_IS_LOADTEST;
					// log.info("apiUrlText:" + apiUrlText);
					if ($WG_IS_TRACE_DETAIL) {
						$WG_TRACE += apiUrlText + "→";
					}

					apiUrlText = XSS_ATTACK_REPLACE_SET(apiUrlText);
					String responseText = wgCallApi(apiUrlText, 5 * ($tryCount + 1));
					// log.info("responseText:" + responseText);

					// 현재 대기자가 있으면 응답문자열에 "WAIT"가 포함, 대기자 수가 없으면 "PASS"가 포함됨
					if (responseText != null && responseText.length() > 0
							&& responseText.indexOf("\"ResultCode\":0") >= 0) {
						if (responseText.indexOf("WAIT") >= 0) {
							$WG_TRACE += "WAIT,";
							$WG_IS_NEED_TO_WAIT = true;
							break;
						} else { // PASS (대기가 없는 경우)
							$WG_TRACE += "PASS,";
							$WG_IS_NEED_TO_WAIT = false;
							break;
						}
					}
				} catch (Exception $e) {
					// ignore & goto next
					$WG_TRACE += "ERROR:" + $e.getMessage() + ",";
				}
			}
			// 코드가 여기까지 왔다는 것은
			// 대기열서버응답에 실패 OR 대기자가 없는("PASS") 상태이므로 원래 페이지를 로드합니다.
			$WG_TRACE += "TryCount:" + $tryCount + ",";
		} else {
			$WG_TRACE += "SKIP,";
		}
		/* end of STEP-3 */

		$WG_TRACE += "→return:" + $WG_IS_NEED_TO_WAIT;

		// write cookie for trace
		wgWriteCookie($RES, "WG_VER_BACKEND", $WG_VERSION);
		wgWriteCookie($RES, "WG_MOD_BACKEND", $WG_MODULE);
		Date now = new Date();
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); // UTC
		String nowText = sf.format(now);
		wgWriteCookie($RES, "WG_TIME", nowText);
		wgWriteCookie($RES, "WG_TRACE", $WG_TRACE);
		wgWriteCookie($RES, "WG_CLIENT_IP", $WG_CLIENT_IP);
		wgWriteCookie($RES, "WG_WAS_IP", $WG_WAS_IP);
		wgWriteCookie($RES, "WG_GATE_ID", $WG_GATE_ID);

		return $WG_IS_NEED_TO_WAIT;
	}

	/* Random String */
	static String wgGetRandomString(int length) {
		StringBuffer buffer = new StringBuffer();
		SecureRandom random = new SecureRandom();

		String chars[] = "1,2,3,4,5,6,7,8,A,B,C,D,E,F,G,H,J,K,L,M,N,P,Q,R,S,T,W,X,Y,Z".split(",");

		for (int i = 0; i < length; i++) {
			buffer.append(chars[random.nextInt(chars.length)]);
		}
		return buffer.toString();
	}

	/* 쿠키 GET */
	static String wgReadCookie(HttpServletRequest req, String key) {
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

	/* 쿠키 SET */
	static void wgWriteCookie(HttpServletResponse res, String key, String value) {
		// default cookie (auto domain)
		try {
			String cookieValue = value;
			Cookie cookie = new Cookie(key, URLEncoder.encode(value, "UTF-8"));
			cookie.setMaxAge(86400 * 1);
			cookie.setPath("/");
			res.addCookie(cookie);
		} catch (Exception ex) {
			// skip
		}
	}

	/* API CALL */
	static String wgCallApi(String urlText, int timeoutSeconds) {
		try {
			URL url = new URL(urlText);
			URLConnection con = url.openConnection();
			con.setConnectTimeout(timeoutSeconds * 1000); // 대기열 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;
			con.setReadTimeout(timeoutSeconds * 1000); // 대깅려 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;

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
	// end of sample code

	
	
	
	








	
	
	
	/*
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 이 함수는
	 * 운영서버에 구현되어 있는 함수의 개발용 DUMMY입니다. 운영서버에 포함되지 않도록 주의 요망!
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	public static String XSS_ATTACK_REPLACE_SET(String url) {
		return url;
	}

	public static String WG_GetWaitingUi(String serviceId, String gateId) {
		String versionTag = "";
		Date nowDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00");
		versionTag = sdf.format(nowDate);

		String html = "" + "<!DOCTYPE html>\r\n" + "<html>\r\n" + "<head>\r\n"
				+ "    <meta http-equiv='X-UA-Compatible' content='IE=edge'/>\r\n" + "    <meta charset='utf-8'/>\r\n"
				+ "    <meta http-equiv='cache-control' content='no-cache' />\r\n"
				+ "    <meta http-equiv='Expires' content='-1'/>\r\n"
				+ "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'/>\r\n"
				+ "    <title></title>\r\n" + "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v="
				+ versionTag + "' rel='stylesheet'>\r\n" + "</head>\r\n" + "<body>\r\n"
				+ "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v="
				+ versionTag + "'></script>\r\n" + "    <script>\r\n"
				+ "        window.addEventListener('load', function () {\r\n"
				+ "            WG_StartWebGate('WG_GATE_ID', window.location.href, 'BACKEND'); //reload \r\n"
				+ "        });\r\n" + "    </script>\r\n" + "</body>\r\n" + "</html>";

		return html.replaceAll("WG_SERVICE_ID", serviceId).replaceAll("WG_GATE_ID", gateId);

	}

}