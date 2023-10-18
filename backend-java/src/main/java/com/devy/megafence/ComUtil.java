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


public class ComUtil {
	private Logger log = LoggerFactory.getLogger(ComUtil.class);

	public ComUtil() {
		// jsp 소스와 동일하게 만들기 위해 Class 기능은 사용하지 않고, 함수 기능 위주로 구현
	}

	/* ################################################################################################################
	 * 업데이트(A안) 안내
	 * ################################################################################################################ 
	 * 	
	 * 기존 사용 모듈(V.21)에서 변경을 최소한 버젼으로, 주요 변경 내용은 아래와 같습니다.
	 * 		1. Random() --> SecureRandom()으로 함수 변경 
	 *  	2. API SERVER IP 배열(WG_GATE_SERVERS) 축소 : 12개 --> 6개 (서비스 성능 영향 없음)   
	 *  	3. API Protocol 변경 : http --> https
	 *  
	 *  <업데이트 방법>
	 *  isWatingMegaFence() 함수 교체 : 아래 소스 50 ~ 162 line 해당
	 */
	
	
	/*
	 * 대기열 체크 (대기여부 판단)
	 */
	public static boolean isWatingMegaFence(HttpServletRequest request, HttpServletResponse response, String gateId)
			throws Exception {

		String WG_GATE_ID = gateId; // 부하테스트용 GATE ID
		String WG_SERVICE_ID = "1011"; // fixed
		String WG_SECRET_TEXT = "XXXX"; // fixed
		String WG_VALIDATION_KEY = WG_SERVICE_ID + "-" + WG_GATE_ID + "-" + WG_SECRET_TEXT;
		String WG_COOKIE_NAME = "WG_VALIDATION_KEY";
		String WG_GATE_SERVERS[] = { 
				"1011-01.devy.kr", "1011-02.devy.kr", "1011-03.devy.kr", 
				"1011-04.devy.kr", "1011-05.devy.kr", "1011-06.devy.kr" 
				};
		int WG_RETRY_COUNT = 3; // failover retry count
		
		Boolean wg_is_need_to_redirect = true;

		/* begin of 쿠키검증 */
		// 쿠키("WG_VALIDATION_KEY")의 값이 검증키와 같은지 체크해서 wg_is_need_to_redirect set
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals(WG_COOKIE_NAME) && cookies[i].getValue().equals(WG_VALIDATION_KEY)) {
					// 검증키가 일치하면 OK (redirect 필요 없음)
					wg_is_need_to_redirect = false;

					// 체크 완료 시 페이지 새로고침 시에도 대기열을 다시 체크하기위해 검증키 쿠키 삭제
					cookies[i].setMaxAge(0);
					cookies[i].setPath("/");
					response.addCookie(cookies[i]);
					break;
				}
			}
		}
		/* end of 쿠키검증 */

		/* begin of 대기열UI 표시여부 판단 */
		if (wg_is_need_to_redirect) {
			/*
			 * 검증키가 Cookie에 없거나 일치하지 않아서 쿠키 검증에 실패하면(wg_is_need_to_redirect == true) 이곳으로
			 * 진입합니다. 대기열서버에 WEB-API를 호출하여 응답 내용으로 대기열UI 표시 여부(PASS/WAIT)를 판단합니다. --> 대기자가
			 * 없으면 응답에 "PASS" 포함됨(정상 페이지로드하면 됨) --> 대기자가 있으면 응답에 "WAIT" 포함됨(응답을
			 * _waiting.html의 html로 교체)
			 */

			Boolean wg_isWaiting = false; // 현재 대기자가 있는지 여부

			SecureRandom wg_rand = new SecureRandom();
			int wg_serverChoice = wg_rand.nextInt(WG_GATE_SERVERS.length); // 0 ~ 11(=서버IP수 -1)

			// Fail-over를 위해 최대 3차까지 시도
			for (int i = 0; i < WG_RETRY_COUNT; i++) {
				try {
					// WG_GATE_SERVERS 서버 중 임의의 서버에 API 호출 --> json 응답
					String wg_receiveLine = "";
					String wg_receiveText = "";

					// 임의의 대기열 서버 선택하여 대기상태 확인(대기해야 하는지 web-api로 확인)하기 위한 URL SET
					// int wg_serverChoice = wg_rand.nextInt(WG_GATE_SERVERS.length) + 0;
					int idx = (wg_serverChoice + i) % WG_GATE_SERVERS.length; // retry 될 때마다 index값 1씩 증가하며 다음 서버 선택

					String url = "https://" + WG_GATE_SERVERS[idx] + "/?ServiceId=" + WG_SERVICE_ID + "&GateId="
							+ WG_GATE_ID + "&Action=CHECK";
					url = XSS_ATTACK_REPLACE_SET(url);
					URL wg_url = new URL(url);

					// 이 페이지가 부하테스트(LoadTest)용으로 호출된 경우를 대비해서 아래와 같은 처리를 추가합니다.
					// 부하테스트 시 IsLoadTest=Y paramter를 URL에 추가하여 대기열 통계가 정확하게 계산되도록 합니다. (일반적인경우에는
					// 상관없음)
					if (request.getParameter("IsLoadTest") != null && request.getParameter("IsLoadTest").equals("Y")) {
						url = "https://" + WG_GATE_SERVERS[idx] + "/?ServiceId=" + WG_SERVICE_ID + "&GateId="
								+ WG_GATE_ID + "&Action=CHECK&IsLoadTest=Y";
						url = XSS_ATTACK_REPLACE_SET(url);
						wg_url = new URL(url);
					}

					// WEB-API 호출
					URLConnection wg_con = wg_url.openConnection();
					wg_con.setConnectTimeout(1000); // 대기열 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;
					wg_con.setReadTimeout(1000); // 대기열 서버 통신 오류로 인해 접속 지연시 강제로 timeout 처리;

					// WEB-API 응답 수신
					BufferedReader wg_readBuffer = new BufferedReader(new InputStreamReader(wg_con.getInputStream()));
					while ((wg_receiveLine = wg_readBuffer.readLine()) != null) {
						wg_receiveText += wg_receiveLine;
					}
					wg_readBuffer.close();

					// 응답 Text에 "PASS" 포함되어 있는지 체크하여 대기할지 판단(PASS/WAIT)
					if (wg_receiveText.length() > 0) {
						if (wg_receiveText.indexOf("PASS") >= 0) {
							wg_isWaiting = false;
						} else {
							wg_isWaiting = true;
						}

						// 현재 대기자가 있는 경우 WAIT(대기열 UI 표시) 처리 : 응답을 LoadWebGate.html의 html로 교체
						// 현재 대기자가 없는 경우 원래의 컨텐츠를 load하기 위해 loop exit
						return wg_isWaiting;

					}
				} catch (Exception e) {
					// 오류 시 오류 무시하고 재시도 (failover)
					Logger log = LoggerFactory.getLogger(ComUtil.class);
					log.debug("isWatingMegaFence err");
				}
			}

			// 코드가 여기까지 왔다는 것은
			// 대기열서버와 3회 이상 통신실패 OR 대기자가 없는("PASS") 상태이므로 원래 페이지를 로드합니다.
		}
		return false;
		/* end of 대기열UI 표시여부 판단 */
	}













	/*
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
	 * 여기서 부터는 개발용 및 TEST용 DUMMY입니다. 
	 * 운영 소스에 포함되지 않도록 해주세요.
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
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

	public static String XSS_ATTACK_REPLACE_SET(String url) {
		return url;
	}
	
	
	
}